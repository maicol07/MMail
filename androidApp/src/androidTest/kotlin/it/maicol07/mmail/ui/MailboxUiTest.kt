package it.maicol07.mmail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import it.maicol07.mmail.App
import it.maicol07.mmail.data.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MailboxUiTest(
    private val languageTag: String,
    private val unread: String,
    private val filters: String,
    private val showMessages: String,
    private val activeFilters: String,
    private val back: String,
    private val attachments: String,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun locales(): List<Array<String>> = listOf(
            arrayOf("en", "Unread", "Filters", "Show messages", "Filters: Unread", "Back to messages", "Open attachments in Outlook"),
            arrayOf("it", "Da leggere", "Filtri", "Mostra messaggi", "Filtri: Da leggere", "Torna ai messaggi", "Apri allegati in Outlook"),
        )
    }

    @get:Rule
    val compose = createComposeRule()
    private val provider = TestMailProvider()
    private val originalLocale = java.util.Locale.getDefault()

    @Before
    fun setResourceLocale() {
        // Compose Resources uses Locale.current in addition to the overridden Android configuration.
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag(languageTag))
    }

    @After
    fun restoreResourceLocale() {
        java.util.Locale.setDefault(originalLocale)
    }

    private fun showApp() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.Locales(LocaleList(languageTag))) {
                Box(Modifier.requiredSize(400.dp, 800.dp)) { App(provider) }
            }
        }
    }

    @Test
    fun filterSheetAndUnreadNavigationKeepTheOpenedMessageVisible() {
        showApp()
        compose.onNodeWithText(unread).assertDoesNotExist()
        compose.onNodeWithContentDescription(filters).performClick()
        compose.onNodeWithText(unread).performClick()
        compose.onNodeWithText(showMessages).performClick()
        compose.onNodeWithContentDescription(activeFilters).assertExists()
        compose.onNodeWithText(provider.message.subject).performClick()
        compose.onNodeWithContentDescription(back).assertIsDisplayed()
        compose.onNodeWithContentDescription(activeFilters).assertDoesNotExist()
        compose.onNodeWithText(provider.message.body).assertExists()
        compose.onNodeWithContentDescription(back).performClick()
        compose.onNodeWithText(provider.message.subject).assertDoesNotExist()
    }

    @Test
    fun olderMessagesLoadAtTheEndWithoutBlockingNavigation() {
        provider.nextPageGate = CompletableDeferred()
        showApp()
        compose.runOnIdle { assertEquals(0, provider.pagesLoaded) }
        compose.onNodeWithTag("message-list").performScrollToIndex(20)
        compose.waitUntil { provider.pagesLoaded == 1 }
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithContentDescription(filters).performClick()
        compose.onNodeWithText(showMessages).assertIsDisplayed().performClick()
        compose.runOnIdle { provider.nextPageGate?.complete(Unit) }
        compose.onNodeWithTag("message-list").performScrollToNode(hasText(provider.olderMessage.subject))
        compose.onNodeWithText(provider.olderMessage.subject).assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, provider.pagesLoaded) }
    }

    @Test
    fun failedPaginationStopsAndPullToRefreshRemainsAvailable() {
        provider.failNextPage = true
        showApp()
        compose.onNodeWithTag("message-list").performScrollToIndex(20)
        compose.waitUntil { provider.pagesLoaded == 1 }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, provider.pagesLoaded) }
        compose.onNodeWithTag("message-list").performScrollToIndex(0)
        compose.onNodeWithTag("message-list").performTouchInput { swipeDown() }
        compose.waitUntil { provider.mailboxLoads == 2 }
    }

    @Test
    fun sharedTransitionRoundTripAndAttachmentChipRemainInteractive() {
        showApp()
        compose.onNodeWithText(provider.message.subject).performClick()
        compose.onNodeWithText(attachments).performScrollTo().assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(1, provider.attachmentsOpened) }
        compose.onNodeWithContentDescription(back).performClick()
        compose.onNodeWithText(provider.message.subject).assertIsDisplayed()
        compose.onNodeWithText(attachments).assertHasClickAction()
    }
}

private class TestMailProvider : MailProvider {
    val message = Mail("test-id", "Test sender", "sender@example.com", "Test subject", "Test body", "10:00", unread = true, hasAttachments = true)
    val olderMessage = message.copy(id = "older-id", subject = "Older subject", unread = false, hasAttachments = false)
    private var mailbox = MailboxState(listOf(message.copy(body = "Preview")) + (1..19).map {
        message.copy(id = "page-$it", subject = "Message $it", unread = false, hasAttachments = false)
    }, mapOf(Folder.Inbox to "next-page"))
    var attachmentsOpened = 0
    var pagesLoaded = 0
    var mailboxLoads = 0
    var failNextPage = false
    var nextPageGate: CompletableDeferred<Unit>? = null
    override val configured = true
    override suspend fun restoreAccount(): String = "test@example.com"
    override suspend fun signIn(): String = "test@example.com"
    override suspend fun signOut() = Unit
    override suspend fun loadMailbox(): MailboxState {
        mailboxLoads++
        return mailbox
    }
    override suspend fun loadMore(mailbox: MailboxState, folder: Folder): MailboxState {
        pagesLoaded++
        nextPageGate?.await()
        if (failNextPage) throw MailProviderException(MailFailure.Network)
        this.mailbox = mailbox.copy(messages = mailbox.messages + olderMessage, nextPages = mailbox.nextPages - folder)
        return this.mailbox
    }
    override suspend fun loadMessage(mail: Mail): Mail = mail.copy(body = message.body)
    override suspend fun markRead(id: String) { mailbox = mailbox.markRead(id) }
    override suspend fun setStarred(id: String, starred: Boolean) { mailbox = mailbox.toggleStar(id) }
    override suspend fun move(id: String, destination: Folder) { mailbox = mailbox.move(id, destination) }
    override suspend fun send(recipient: String, subject: String, body: String, replyTo: String?) = Unit
    override fun openAttachments(mail: Mail) { attachmentsOpened++ }
}
