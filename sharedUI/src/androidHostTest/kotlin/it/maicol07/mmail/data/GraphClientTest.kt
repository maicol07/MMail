package it.maicol07.mmail.data

import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject

class GraphClientTest {
    private fun message(): JSONObject = JSONObject("""{
        "id":"immutable/id=","from":{"emailAddress":{"name":"Sender","address":"sender@example.com"}},
        "toRecipients":[{"emailAddress":{"address":"recipient@example.com"}}],
        "subject":"Subject","bodyPreview":"Preview","body":{"contentType":"text","content":"Full body"},
        "receivedDateTime":"2026-10-24T10:00:00Z","isRead":false,
        "flag":{"flagStatus":"flagged"},"hasAttachments":true,"webLink":"https://outlook.office.com/mail/test"
    }""")

    private fun page(message: JSONObject, next: String? = null): MockResponse {
        val json = JSONObject().put("value", org.json.JSONArray().put(message))
        if (next != null) json.put("@odata.nextLink", next)
        return MockResponse().setBody(json.toString())
    }

    @Test
    fun decodesGraphFieldsAndRejectsMalformedResponses() {
        val mail = decodeMail(message(), Folder.Inbox, "Full body")
        assertEquals("immutable/id=", mail.id)
        assertEquals("Full body", mail.body)
        assertEquals("recipient@example.com", mail.recipient)
        assertTrue(mail.unread && mail.starred && mail.hasAttachments)
        assertEquals(MailFailure.InvalidResponse, assertFailsWith<MailProviderException> {
            decodeMail(message().put("receivedDateTime", "invalid"), Folder.Inbox, "Full body")
        }.failure)
        assertFailsWith<MailProviderException> { decodeMail(message().put("id", ""), Folder.Inbox, "Full body") }
    }

    @Test
    fun defersPaginationAndBodiesUntilRequested() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(page(message(), server.url("/v1.0/page2").toString()))
            repeat(3) { server.enqueue(MockResponse().setBody("{\"value\":[]}")) }
            val client = GraphClient({ "test-token" }, server.url("/v1.0/"))
            val mailbox = client.loadMailbox()
            assertEquals(listOf("immutable/id="), mailbox.messages.map { it.id })
            assertEquals("Preview", mailbox.messages.single().body)
            assertTrue(mailbox.hasMore(Folder.Inbox))
            assertEquals(4, server.requestCount)
            val request = server.takeRequest(1, TimeUnit.SECONDS)!!
            assertEquals("Bearer test-token", request.getHeader("Authorization"))
            assertTrue(request.getHeader("Prefer")!!.contains("ImmutableId"))
            assertTrue(request.getHeader("Prefer")!!.contains("body-content-type=\"text\""))
            assertEquals("receivedDateTime desc", request.requestUrl!!.queryParameter("\$orderby"))
            assertTrue("body" !in request.requestUrl!!.queryParameter("\$select")!!.split(","))
            repeat(3) { server.takeRequest() }
            server.enqueue(page(message().put("id", "second")))
            val expanded = client.loadMore(mailbox, Folder.Inbox)
            assertEquals(listOf("immutable/id=", "second"), expanded.messages.map { it.id })
            assertTrue(!expanded.hasMore(Folder.Inbox))
            assertEquals("/v1.0/page2", server.takeRequest().path)
            assertEquals(5, server.requestCount)
            assertEquals(expanded, client.loadMore(expanded, Folder.Inbox))
            assertEquals(5, server.requestCount)
        }
    }

    @Test
    fun loadsFullBodySeparatelyAndValidatesItsFormatAndIdentity() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val client = GraphClient({ "test-token" }, server.url("/v1.0/"))
            val summary = decodeMail(message(), Folder.Inbox, "Preview")
            server.enqueue(MockResponse().setBody(message().toString()))
            assertEquals("Full body", client.loadMessage(summary).body)
            val request = server.takeRequest()
            assertEquals("/v1.0/me/messages/immutable%2Fid=", request.path)
            assertTrue(request.getHeader("Prefer")!!.contains("body-content-type=\"text\""))
            server.enqueue(MockResponse().setBody(message().put("body", JSONObject().put("contentType", "html").put("content", "<b>Body</b>")).toString()))
            assertEquals(MailFailure.InvalidResponse, assertFailsWith<MailProviderException> { client.loadMessage(summary) }.failure)
            server.enqueue(MockResponse().setBody(message().put("id", "different").toString()))
            assertEquals(MailFailure.InvalidResponse, assertFailsWith<MailProviderException> { client.loadMessage(summary) }.failure)
        }
    }

    @Test
    fun refusesForeignPaginationLinksBeforeRequestingAnotherToken() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(page(message(), "https://attacker.example/v1.0/messages"))
            repeat(3) { server.enqueue(MockResponse().setBody("{\"value\":[]}")) }
            var tokens = 0
            val client = GraphClient({ tokens++; "test-token" }, server.url("/v1.0/"))
            val mailbox = client.loadMailbox()
            assertEquals(MailFailure.InvalidResponse, assertFailsWith<MailProviderException> { client.loadMore(mailbox, Folder.Inbox) }.failure)
            assertEquals(4, tokens)
            assertEquals(4, server.requestCount)
        }
    }

    @Test
    fun mutationsEncodeIdsAndUseGraphPayloads() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            repeat(5) { server.enqueue(MockResponse().setResponseCode(202)) }
            val client = GraphClient({ "test-token" }, server.url("/v1.0/"))
            client.markRead("immutable/id=")
            client.setStarred("immutable/id=", true)
            client.move("immutable/id=", Folder.Trash)
            client.send("recipient@example.com", "Subject", "Body", null)
            client.send("recipient@example.com", "Re: Subject", "Reply", "immutable/id=")
            val read = server.takeRequest()
            assertEquals("PATCH", read.method)
            assertEquals("/v1.0/me/messages/immutable%2Fid=", read.path)
            assertTrue(JSONObject(read.body.readUtf8()).getBoolean("isRead"))
            assertEquals("flagged", JSONObject(server.takeRequest().body.readUtf8()).getJSONObject("flag").getString("flagStatus"))
            assertEquals("deleteditems", JSONObject(server.takeRequest().body.readUtf8()).getString("destinationId"))
            val send = server.takeRequest()
            assertEquals("/v1.0/me/sendMail", send.path)
            assertTrue(JSONObject(send.body.readUtf8()).getBoolean("saveToSentItems"))
            assertTrue(server.takeRequest().path!!.endsWith("/reply"))
        }
    }

    @Test
    fun retriesInterruptedReadsButNeverInterruptedSends() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
            repeat(4) { server.enqueue(MockResponse().setBody("{\"value\":[]}")) }
            val client = GraphClient({ "test-token" }, server.url("/v1.0/"))
            assertTrue(client.loadMailbox().messages.isEmpty())
            assertEquals(5, server.requestCount)

            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
            assertEquals(MailFailure.SendUncertain, assertFailsWith<MailProviderException> {
                client.send("recipient@example.com", "Subject", "Body", null)
            }.failure)
            assertEquals(6, server.requestCount)
        }
    }

    @Test
    fun stopsAfterThreeInterruptedReads() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            repeat(3) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)) }
            val client = GraphClient({ "test-token" }, server.url("/v1.0/"))
            assertEquals(MailFailure.Network, assertFailsWith<MailProviderException> { client.loadMailbox() }.failure)
            assertEquals(3, server.requestCount)
        }
    }

    @Test
    fun failuresAreSpecificAndSendingIsNeverAutomaticallyRetried() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val client = GraphClient({ "test-token" }, server.url("/v1.0/"))
            for ((status, failure) in listOf(401 to MailFailure.Authentication, 429 to MailFailure.Throttled, 403 to MailFailure.Rejected)) {
                server.enqueue(MockResponse().setResponseCode(status))
                assertEquals(failure, assertFailsWith<MailProviderException> { client.markRead("id") }.failure)
            }
            server.enqueue(MockResponse().setResponseCode(503))
            assertEquals(MailFailure.SendUncertain, assertFailsWith<MailProviderException> {
                client.send("recipient@example.com", "Subject", "Body", null)
            }.failure)
            assertEquals(4, server.requestCount)
            assertFailsWith<IllegalArgumentException> { client.send("invalid", "Subject", "Body", null) }
            assertEquals(4, server.requestCount)
        }
    }
}
