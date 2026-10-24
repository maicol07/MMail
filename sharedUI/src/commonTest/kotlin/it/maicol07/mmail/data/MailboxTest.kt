package it.maicol07.mmail.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MailboxTest {
    @Test
    fun searchCombinesFolderQueryAndFilter() {
        val state = MailboxState()
        assertEquals(listOf(1), state.messagesIn(Folder.Inbox, "  SOFIA  ", MailFilter.Attachments).map { it.id })
        assertTrue(state.messagesIn(Folder.Inbox, "nonexistent", MailFilter.All).isEmpty())
        assertTrue(state.messagesIn(Folder.Inbox, "", MailFilter.Unread).all { it.unread })
        assertEquals(listOf(8), state.messagesIn(Folder.Sent, "andrea@example.com", MailFilter.All).map { it.id })
    }

    @Test
    fun readingStarringAndMovingPreserveOtherMessages() {
        val initial = MailboxState()
        val changed = initial.markRead(1).toggleStar(1).move(1, Folder.Archive)
        assertFalse(changed.messages.first().unread)
        assertFalse(changed.messages.first().starred)
        assertEquals(Folder.Archive, changed.messages.first().folder)
        assertEquals(initial.messages.drop(1), changed.messages.drop(1))
        val trashed = initial.move(1, Folder.Trash)
        assertFalse(trashed.messagesIn(Folder.Starred, "", MailFilter.All).any { it.id == 1 })
    }

    @Test
    fun simulatedSendAddsOnlyToSentAndValidatesInput() {
        val state = MailboxState().send("  luca@example.com ", "  Hello  ", "A test")
        assertEquals(11, state.messages.size)
        assertEquals("luca@example.com", state.messages.first().recipient)
        assertEquals("Hello", state.messages.first().subject)
        assertEquals(Folder.Sent, state.messages.first().folder)
        assertEquals(2, state.messagesIn(Folder.Sent, "", MailFilter.All).size)
        assertFailsWith<IllegalArgumentException> { state.send("address", "Hello", "Body") }
        assertFailsWith<IllegalArgumentException> { state.send("luca@example.com", " ", "Body") }
        assertFailsWith<IllegalArgumentException> { state.send("luca@example.com", "Hello", " ") }
    }
}
