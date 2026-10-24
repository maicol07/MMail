package it.maicol07.mmail.data

enum class Folder { Inbox, Starred, Sent, Archive, Trash }
enum class MailFilter { All, Unread, Attachments }

data class Mail(
    val id: Int,
    val sender: String,
    val address: String,
    val subject: String,
    val body: String,
    val time: String,
    val folder: Folder = Folder.Inbox,
    val unread: Boolean = false,
    val starred: Boolean = false,
    val attachment: String? = null,
    val recipient: String = "maicol@example.com",
)

data class MailboxState(val messages: List<Mail> = demoMessages) {
    fun messagesIn(folder: Folder, query: String, filter: MailFilter): List<Mail> = messages.filter { mail ->
        val inFolder = if (folder == Folder.Starred) mail.starred && mail.folder != Folder.Trash else mail.folder == folder
        inFolder && (query.isBlank() || listOf(mail.sender, mail.address, mail.subject, mail.body, mail.recipient)
            .any { it.contains(query.trim(), ignoreCase = true) }) && when (filter) {
            MailFilter.All -> true
            MailFilter.Unread -> mail.unread
            MailFilter.Attachments -> mail.attachment != null
        }
    }

    fun markRead(id: Int): MailboxState = copy(messages = messages.map { if (it.id == id) it.copy(unread = false) else it })
    fun toggleStar(id: Int): MailboxState = copy(messages = messages.map { if (it.id == id) it.copy(starred = !it.starred) else it })
    fun move(id: Int, folder: Folder): MailboxState = copy(messages = messages.map { if (it.id == id) it.copy(folder = folder) else it })

    fun send(recipient: String, subject: String, body: String): MailboxState {
        require(isValidRecipient(recipient)) { "Invalid recipient" }
        require(subject.isNotBlank() && body.isNotBlank()) { "Subject and body are required" }
        val message = Mail(
            id = (messages.maxOfOrNull { it.id } ?: 0) + 1,
            sender = "Maicol", address = "maicol@example.com", subject = subject.trim(), body = body.trim(),
            time = "Now", folder = Folder.Sent, recipient = recipient.trim(),
        )
        return copy(messages = listOf(message) + messages)
    }
}

fun isValidRecipient(value: String): Boolean = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value.trim())

val demoMessages: List<Mail> = listOf(
    Mail(1, "Sofia Bianchi", "sofia@example.com", "A little trip, a big idea",
        "Hi Maicol,\n\nI put together a few ideas for our weekend at the lake. I was thinking of two relaxed days: a walk, a good lunch, and finally some time for ourselves.\n\nI found a small place in Bellagio with a terrace overlooking the lake. I am sending you the itinerary, but we can also decide as we go.\n\nHow about leaving on Saturday morning?\n\nSee you soon,\nSofia", "10:42", unread = true, starred = true, attachment = "Lake weekend.pdf"),
    Mail(2, "Luca Moretti", "luca@example.com", "The next step for MMail",
        "Hi Maicol,\n\nI reviewed the project screens. I like the idea of giving messages more space and reducing the noise around them.\n\nCan we talk on Monday? I would like to compare two proposals for tablet navigation.\n\nThanks,\nLuca", "09:18", unread = true, attachment = "Project notes.pdf"),
    Mail(3, "Studio Forma", "hello@forma.example.com", "Room for ideas, even on your desk",
        "Hi Maicol,\n\nThis month at the studio, we are talking about spaces that help us focus. A few objects, natural light, and the right notebook.\n\nStop by on Friday for a coffee and a few new ideas.\n\nThe Forma team", "08:30", unread = true),
    Mail(4, "Giulia Rossi", "giulia@example.com", "Meet for coffee?",
        "Hi!\n\nI will be downtown tomorrow afternoon. If you are free, shall we grab a coffee at our usual place?\n\nI have a couple of things to tell you.\n\nGiulia", "Yesterday", starred = true),
    Mail(5, "Marco Ferri", "marco@example.com", "Sunday's photos",
        "Hi Maicol,\n\nWhat a lovely day on Sunday! Here is a selection of photos from our walk. The one with the light through the trees is my favorite.\n\nUntil next time,\nMarco", "Yesterday", attachment = "Sunday photos.zip"),
    Mail(6, "Design Notes", "letters@design.example.com", "Simple things work better",
        "This week, a reflection on everyday design.\n\nA well-designed interface leaves room for what matters. A good hierarchy, clear words, and predictable interactions already go a long way.\n\nEnjoy the read,\nDesign Notes", "Yesterday"),
    Mail(7, "Elena Conti", "elena@example.com", "All set for Monday",
        "Hi,\n\nI booked the room for 10 and updated the agenda. If you want to add anything, just reply to this email.\n\nHave a good weekend!\nElena", "Oct 22", unread = true),
    Mail(8, "Maicol", "maicol@example.com", "Re: A fresh start",
        "Hi Andrea,\n\nThanks for the summary. I am happy to go ahead with the first proposal.\n\nLet's catch up next week,\nMaicol", "Yesterday", folder = Folder.Sent, recipient = "andrea@example.com"),
    Mail(9, "Public library", "events@library.example.com", "A meeting between the pages",
        "Hi Maicol,\n\nA reminder about Thursday's reading group at 6 pm. The theme will be travel in contemporary fiction.\n\nSee you there!", "Oct 20", folder = Folder.Archive),
    Mail(10, "Demo offers", "offers@example.com", "An old newsletter",
        "This is a demo message in the trash. You can move it back to the inbox.", "Oct 19", folder = Folder.Trash),
)
