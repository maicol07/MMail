package it.maicol07.mmail.data

enum class Folder { Inbox, Starred, Sent, Archive, Trash }
enum class MailFilter { All, Unread, Attachments }

data class Mail(
    val id: String,
    val sender: String,
    val address: String,
    val subject: String,
    val body: String,
    val time: String,
    val folder: Folder = Folder.Inbox,
    val unread: Boolean = false,
    val starred: Boolean = false,
    val hasAttachments: Boolean = false,
    val recipient: String = "",
    val webLink: String = "",
    val htmlBody: String? = null,
)

data class MailboxState(
    val messages: List<Mail> = emptyList(),
    val nextPages: Map<Folder, String> = emptyMap(),
) {
    fun hasMore(folder: Folder): Boolean = if (folder == Folder.Starred) nextPages.keys.any { it != Folder.Trash } else folder in nextPages

    fun messagesIn(folder: Folder, query: String, filter: MailFilter): List<Mail> = messages.filter { mail ->
        val inFolder = if (folder == Folder.Starred) mail.starred && mail.folder != Folder.Trash else mail.folder == folder
        inFolder && (query.isBlank() || listOf(mail.sender, mail.address, mail.subject, mail.body, mail.recipient)
            .any { it.contains(query.trim(), ignoreCase = true) }) && when (filter) {
            MailFilter.All -> true
            MailFilter.Unread -> mail.unread
            MailFilter.Attachments -> mail.hasAttachments
        }
    }

    fun markRead(id: String): MailboxState = copy(messages = messages.map { if (it.id == id) it.copy(unread = false) else it })
    fun toggleStar(id: String): MailboxState = copy(messages = messages.map { if (it.id == id) it.copy(starred = !it.starred) else it })
    fun move(id: String, folder: Folder): MailboxState = copy(messages = messages.map { if (it.id == id) it.copy(folder = folder) else it })
}

fun isValidRecipient(value: String): Boolean = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value.trim())
