package it.maicol07.mmail.data

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import java.io.File
import java.io.IOException
import java.net.SocketException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class OutlookProvider(
    private val activity: Activity,
    private val clientId: String,
    private val signatureHash: String,
) : MailProvider {
    override val configured: Boolean = clientId.matches(Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) &&
        signatureHash.matches(Regex("[A-Za-z0-9+/]{27}="))
    private var application: ISingleAccountPublicClientApplication? = null
    private var account: IAccount? = null
    private val scopes = listOf("https://graph.microsoft.com/Mail.ReadWrite", "https://graph.microsoft.com/Mail.Send")
    private val graph = GraphClient(token = { accessToken() })

    private suspend fun application(): ISingleAccountPublicClientApplication {
        application?.let { return it }
        if (!configured) throw MailProviderException(MailFailure.Authentication)
        val file = withContext(Dispatchers.IO) {
            try {
                val redirect = Uri.Builder().scheme("msauth").authority(activity.packageName).appendPath(signatureHash).build()
                File(activity.cacheDir, "outlook-auth.json").apply {
                    writeText(JSONObject().put("client_id", clientId).put("redirect_uri", redirect.toString())
                        .put("account_mode", "SINGLE").put("authorization_user_agent", "BROWSER")
                        .put("broker_redirect_uri_registered", false)
                        .put("authorities", JSONArray().put(JSONObject().put("type", "AAD")
                            .put("audience", JSONObject().put("type", "AzureADandPersonalMicrosoftAccount").put("tenant_id", "common"))))
                        .put("logging", JSONObject().put("pii_enabled", false).put("logcat_enabled", false)).toString())
                }
            } catch (exception: IOException) {
                throw MailProviderException(MailFailure.Authentication, exception)
            }
        }
        return suspendCancellableCoroutine { continuation ->
            PublicClientApplication.createSingleAccountPublicClientApplication(activity.applicationContext, file,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(app: ISingleAccountPublicClientApplication) {
                        application = app
                        if (continuation.isActive) continuation.resume(app)
                    }
                    override fun onError(exception: MsalException) {
                        if (continuation.isActive) continuation.resumeWithException(MailProviderException(MailFailure.Authentication, exception))
                    }
                })
        }
    }

    override suspend fun restoreAccount(): String? {
        if (!configured) return null
        val app = application()
        return suspendCancellableCoroutine { continuation ->
            app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(currentAccount: IAccount?) {
                    account = currentAccount
                    if (continuation.isActive) continuation.resume(currentAccount?.username)
                }
                override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                    account = currentAccount
                }
                override fun onError(exception: MsalException) {
                    if (continuation.isActive) continuation.resumeWithException(MailProviderException(MailFailure.Authentication, exception))
                }
            })
        }
    }

    override suspend fun signIn(): String {
        Log.d("MMail", "operation=signIn state=started")
        val app = application()
        return suspendCancellableCoroutine { continuation ->
            val parameters = AcquireTokenParameters.Builder().startAuthorizationFromActivity(activity)
                .withScopes(scopes).withCallback(object : AuthenticationCallback {
                    override fun onSuccess(result: IAuthenticationResult) {
                        Log.d("MMail", "operation=signIn state=completed")
                        account = result.account
                        if (continuation.isActive) continuation.resume(result.account.username)
                    }
                    override fun onError(exception: MsalException) {
                        if (continuation.isActive) continuation.resumeWithException(MailProviderException(MailFailure.Authentication, exception))
                    }
                    override fun onCancel() {
                        continuation.cancel(CancellationException("Sign-in cancelled"))
                    }
                }).build()
            app.acquireToken(parameters)
        }
    }

    override suspend fun signOut() {
        val app = application()
        suspendCancellableCoroutine { continuation ->
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    account = null
                    if (continuation.isActive) continuation.resume(Unit)
                }
                override fun onError(exception: MsalException) {
                    if (continuation.isActive) continuation.resumeWithException(MailProviderException(MailFailure.Authentication, exception))
                }
            })
        }
    }

    private suspend fun accessToken(): String {
        Log.d("MMail", "operation=accessToken state=started")
        val app = application()
        val current = account ?: throw MailProviderException(MailFailure.Authentication)
        return suspendCancellableCoroutine { continuation ->
            val parameters = AcquireTokenSilentParameters.Builder().forAccount(current).fromAuthority(current.authority)
                .withScopes(scopes).withCallback(object : SilentAuthenticationCallback {
                override fun onSuccess(result: IAuthenticationResult) {
                    Log.d("MMail", "operation=accessToken state=completed")
                    if (continuation.isActive) continuation.resume(result.accessToken)
                }
                override fun onError(exception: MsalException) {
                    Log.w("MMail", "operation=accessToken error=${exception.errorCode}")
                    if (continuation.isActive) continuation.resumeWithException(MailProviderException(MailFailure.Authentication, exception))
                }
                }).build()
            app.acquireTokenSilentAsync(parameters)
        }
    }

    override suspend fun loadMailbox(): MailboxState {
        Log.d("MMail", "operation=loadMailbox state=started")
        try {
            return graph.loadMailbox().also { Log.d("MMail", "operation=loadMailbox state=completed") }
        } catch (exception: MailProviderException) {
            Log.w("MMail", "operation=loadMailbox failure=${exception.failure} cause=${exception.cause?.javaClass?.simpleName}")
            (exception.cause as? SocketException)?.let { Log.w("MMail", "operation=loadMailbox socketError=${it.message}") }
            throw exception
        }
    }
    override suspend fun markRead(id: String) = graph.markRead(id)
    override suspend fun loadMore(mailbox: MailboxState, folder: Folder): MailboxState = graph.loadMore(mailbox, folder)
    override suspend fun loadMessage(mail: Mail): Mail = graph.loadMessage(mail)
    override suspend fun setStarred(id: String, starred: Boolean) = graph.setStarred(id, starred)
    override suspend fun move(id: String, destination: Folder) = graph.move(id, destination)
    override suspend fun send(recipient: String, subject: String, body: String, replyTo: String?) = graph.send(recipient, subject, body, replyTo)

    override fun openAttachments(mail: Mail) {
        // shortcut: Outlook handles attachments; add native downloads when files must open inside MMail.
        val uri = Uri.parse(mail.webLink)
        if (uri.scheme != "https" || uri.host !in setOf("outlook.live.com", "outlook.office.com", "outlook.office365.com")) {
            throw MailProviderException(MailFailure.InvalidResponse)
        }
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (exception: ActivityNotFoundException) {
            throw MailProviderException(MailFailure.Rejected, exception)
        }
    }
}
