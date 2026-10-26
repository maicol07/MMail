package it.maicol07.mmail.data

import java.io.IOException
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class GraphClient(
    private val token: suspend () -> String,
    private val baseUrl: HttpUrl = "https://graph.microsoft.com/v1.0/".toHttpUrl(),
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS).build()

    suspend fun loadMailbox(): MailboxState {
        var mailbox = MailboxState()
        for (folder in listOf(Folder.Inbox, Folder.Sent, Folder.Archive, Folder.Trash)) {
            val url = baseUrl.newBuilder()
                .addPathSegments("me/mailFolders/${folder.graphName}/messages")
                .addQueryParameter("\$select", "id,from,toRecipients,subject,bodyPreview,receivedDateTime,isRead,flag,hasAttachments,webLink")
                .addQueryParameter("\$orderby", "receivedDateTime desc")
                .addQueryParameter("\$top", "100").build()
            mailbox = loadPage(mailbox, folder, url)
        }
        return mailbox
    }

    suspend fun loadMore(mailbox: MailboxState, folder: Folder): MailboxState {
        val source = if (folder == Folder.Starred) mailbox.nextPages.keys.firstOrNull { it != Folder.Trash } ?: return mailbox else folder
        val next = mailbox.nextPages[source] ?: return mailbox
        val url = try { next.toHttpUrl() } catch (exception: IllegalArgumentException) {
            throw MailProviderException(MailFailure.InvalidResponse, exception)
        }
        return loadPage(mailbox, source, url)
    }

    private suspend fun loadPage(mailbox: MailboxState, folder: Folder, url: HttpUrl): MailboxState {
        val page = request("GET", url)
        try {
            val values = page.getJSONArray("value")
            val messages = (0 until values.length()).map { index ->
                val value = values.getJSONObject(index)
                decodeMail(value, folder, value.getString("bodyPreview"))
            }
            val next = if (page.isNull("@odata.nextLink")) null else page.getString("@odata.nextLink")
            if (next == url.toString()) throw MailProviderException(MailFailure.InvalidResponse)
            return mailbox.copy(
                messages = (mailbox.messages + messages).distinctBy { it.id },
                nextPages = (mailbox.nextPages - folder).let { if (next == null) it else it + (folder to next) },
            )
        } catch (exception: JSONException) {
            throw MailProviderException(MailFailure.InvalidResponse, exception)
        }
    }

    suspend fun loadMessage(mail: Mail): Mail {
        val value = request("GET", messageUrl(mail.id))
        try {
            val body = value.getJSONObject("body")
            if (!body.getString("contentType").equals("text", ignoreCase = true) || value.getString("id") != mail.id) {
                throw MailProviderException(MailFailure.InvalidResponse)
            }
            return decodeMail(value, mail.folder, body.getString("content"))
        } catch (exception: JSONException) {
            throw MailProviderException(MailFailure.InvalidResponse, exception)
        }
    }

    suspend fun markRead(id: String) {
        request("PATCH", messageUrl(id), JSONObject().put("isRead", true))
    }

    suspend fun setStarred(id: String, starred: Boolean) {
        request("PATCH", messageUrl(id), JSONObject().put("flag", JSONObject().put("flagStatus", if (starred) "flagged" else "notFlagged")))
    }

    suspend fun move(id: String, destination: Folder) {
        request("POST", messageUrl(id).newBuilder().addPathSegment("move").build(), JSONObject().put("destinationId", destination.graphName))
    }

    suspend fun send(recipient: String, subject: String, body: String, replyTo: String?) {
        require(isValidRecipient(recipient) && subject.isNotBlank() && body.isNotBlank())
        val message = JSONObject().put("subject", subject.trim())
            .put("body", JSONObject().put("contentType", "Text").put("content", body))
            .put("toRecipients", JSONArray().put(JSONObject().put("emailAddress", JSONObject().put("address", recipient.trim()))))
        if (replyTo == null) {
            request("POST", baseUrl.resolve("me/sendMail")!!, JSONObject().put("message", message).put("saveToSentItems", true), sending = true)
        } else {
            request("POST", messageUrl(replyTo).newBuilder().addPathSegment("reply").build(), JSONObject().put("message", message), sending = true)
        }
    }

    private fun messageUrl(id: String): HttpUrl = baseUrl.newBuilder().addPathSegments("me/messages").addPathSegment(id).build()

    private suspend fun request(method: String, url: HttpUrl, body: JSONObject? = null, sending: Boolean = false): JSONObject {
        // Never send a bearer token to a pagination link outside this Graph endpoint.
        if (url.scheme != baseUrl.scheme || url.host != baseUrl.host || url.port != baseUrl.port ||
            !url.encodedPath.startsWith(baseUrl.encodedPath) || url.username.isNotEmpty() || url.password.isNotEmpty()) {
            throw MailProviderException(MailFailure.InvalidResponse)
        }
        val accessToken = token()
        return withContext(Dispatchers.IO) {
            for (attempt in 0..2) {
                try {
                    val request = Request.Builder().url(url)
                        .header("Authorization", "Bearer $accessToken")
                        .header("Prefer", "IdType=\"ImmutableId\", outlook.body-content-type=\"text\"")
                        .method(method, body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
                    return@withContext client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw MailProviderException(when {
                            response.code == 401 -> MailFailure.Authentication
                            response.code == 429 -> MailFailure.Throttled
                            sending && (response.code >= 500 || response.code == 408) -> MailFailure.SendUncertain
                            else -> MailFailure.Rejected
                        })
                        val text = response.body?.string().orEmpty()
                        if (text.isBlank()) JSONObject() else JSONObject(text)
                    }
                } catch (exception: IOException) {
                    if (method != "GET" || attempt == 2) {
                        throw MailProviderException(if (sending) MailFailure.SendUncertain else MailFailure.Network, exception)
                    }
                    delay(250L shl attempt)
                } catch (exception: JSONException) {
                    throw MailProviderException(MailFailure.InvalidResponse, exception)
                }
            }
            error("Read retry limit exhausted")
        }
    }
}

internal val Folder.graphName: String
    get() = when (this) {
        Folder.Inbox -> "inbox"
        Folder.Sent -> "sentitems"
        Folder.Archive -> "archive"
        Folder.Trash -> "deleteditems"
        Folder.Starred -> error("Starred is a virtual folder")
    }

internal fun decodeMail(value: JSONObject, folder: Folder, body: String): Mail {
    try {
        val id = value.getString("id")
        if (id.isBlank()) throw MailProviderException(MailFailure.InvalidResponse)
        val sender = value.optJSONObject("from")?.optJSONObject("emailAddress")
        val recipients = value.getJSONArray("toRecipients")
        return Mail(
            id = id,
            sender = sender?.optString("name").orEmpty().ifBlank { sender?.optString("address").orEmpty() },
            address = sender?.optString("address").orEmpty(),
            subject = value.getString("subject"), body = body,
            time = OffsetDateTime.parse(value.getString("receivedDateTime")).atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)),
            folder = folder, unread = !value.getBoolean("isRead"),
            starred = value.getJSONObject("flag").getString("flagStatus") == "flagged",
            hasAttachments = value.getBoolean("hasAttachments"),
            recipient = (0 until recipients.length()).joinToString(", ") { recipients.getJSONObject(it).getJSONObject("emailAddress").getString("address") },
            webLink = value.getString("webLink"),
        )
    } catch (exception: JSONException) {
        throw MailProviderException(MailFailure.InvalidResponse, exception)
    } catch (exception: DateTimeParseException) {
        throw MailProviderException(MailFailure.InvalidResponse, exception)
    }
}
