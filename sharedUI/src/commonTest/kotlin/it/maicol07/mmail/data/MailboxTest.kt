package it.maicol07.mmail.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailboxTest {
    private val first = Mail("immutable/1=", "Sender", "sender@example.com", "Subject", "Body", "10:00", unread = true, starred = true, hasAttachments = true)
    private val second = first.copy(id = "immutable/2=", folder = Folder.Sent, recipient = "recipient@example.com", unread = false)
    private val state = MailboxState(listOf(first, second))

    @Test
    fun virtualStarredPaginationExcludesTrash() {
        val trashOnly = state.copy(nextPages = mapOf(Folder.Trash to "trash-page"))
        assertFalse(trashOnly.hasMore(Folder.Starred))
        assertTrue(trashOnly.hasMore(Folder.Trash))
        val inbox = trashOnly.copy(nextPages = trashOnly.nextPages + (Folder.Inbox to "inbox-page"))
        assertTrue(inbox.hasMore(Folder.Starred))
        assertTrue(inbox.hasMore(Folder.Inbox))
        assertFalse(inbox.hasMore(Folder.Archive))
    }

    @Test
    fun productionMailboxStartsEmpty() {
        assertTrue(MailboxState().messages.isEmpty())
    }

    @Test
    fun searchCombinesFolderQueryAndFilter() {
        assertEquals(listOf(first), state.messagesIn(Folder.Inbox, "  SENDER  ", MailFilter.Attachments))
        assertTrue(state.messagesIn(Folder.Inbox, "nonexistent", MailFilter.All).isEmpty())
        assertEquals(listOf(first), state.messagesIn(Folder.Inbox, "", MailFilter.Unread))
        assertEquals(listOf(second), state.messagesIn(Folder.Sent, "recipient@example.com", MailFilter.All))
    }

    @Test
    fun readingStarringAndMovingPreserveOtherMessagesAndGraphIds() {
        val changed = state.markRead(first.id).toggleStar(first.id).move(first.id, Folder.Archive)
        assertFalse(changed.messages.first().unread)
        assertFalse(changed.messages.first().starred)
        assertEquals(first.id, changed.messages.first().id)
        assertEquals(Folder.Archive, changed.messages.first().folder)
        assertEquals(second, changed.messages.last())
        assertFalse(state.move(first.id, Folder.Trash).messagesIn(Folder.Starred, "", MailFilter.All).any { it.id == first.id })
    }

    @Test
    fun recipientValidationRejectsMalformedAddresses() {
        assertTrue(isValidRecipient("  recipient@example.com  "))
        assertFalse(isValidRecipient("address"))
        assertFalse(isValidRecipient("a@example.com,b@example.com"))
        assertFalse(isValidRecipient("a b@example.com"))
    }
}
