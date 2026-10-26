package it.maicol07.mmail.data

interface MailProvider {
    val configured: Boolean
    suspend fun restoreAccount(): String?
    suspend fun signIn(): String
    suspend fun signOut()
    suspend fun loadMailbox(): MailboxState
    suspend fun loadMore(mailbox: MailboxState, folder: Folder): MailboxState
    suspend fun loadMessage(mail: Mail): Mail
    suspend fun markRead(id: String)
    suspend fun setStarred(id: String, starred: Boolean)
    suspend fun move(id: String, destination: Folder)
    suspend fun send(recipient: String, subject: String, body: String, replyTo: String?)
    fun openAttachments(mail: Mail)
}

enum class MailFailure { Authentication, Network, Throttled, Rejected, InvalidResponse, SendUncertain }

class MailProviderException(val failure: MailFailure, cause: Throwable? = null) : Exception(failure.name, cause)
