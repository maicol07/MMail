package it.maicol07.mmail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import it.maicol07.mmail.App
import it.maicol07.mmail.data.demoMessages
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
    private val attachmentNotice: String,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun locales(): List<Array<String>> = listOf(
            arrayOf("en", "Unread", "Filters", "Show messages", "Filters: Unread", "Back to messages",
                "Attachments are examples and contain no actual files."),
            arrayOf("it", "Da leggere", "Filtri", "Mostra messaggi", "Filtri: Da leggere", "Torna ai messaggi",
                "Gli allegati sono esempi e non contengono file reali."),
        )
    }

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun filterSheetAndUnreadNavigationKeepTheOpenedMessageVisible() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.Locales(LocaleList(languageTag))) {
                Box(Modifier.requiredSize(400.dp, 800.dp)) { App() }
            }
        }
        compose.onNodeWithText(unread).assertDoesNotExist()
        compose.onNodeWithContentDescription(filters).performClick()
        compose.onNodeWithText(unread).performClick()
        compose.onNodeWithText(showMessages).performClick()
        compose.onNodeWithContentDescription(activeFilters).assertExists()

        compose.onNodeWithText(demoMessages.first().subject).performClick()
        compose.onNodeWithContentDescription(back).assertIsDisplayed()
        compose.onNodeWithText(demoMessages.first().body).assertExists()
        compose.onNodeWithContentDescription(back).performClick()
        compose.onNodeWithText(demoMessages.first().subject).assertDoesNotExist()
    }

    @Test
    fun sharedTransitionRoundTripAndAttachmentChipRemainInteractive() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.Locales(LocaleList(languageTag))) {
                Box(Modifier.requiredSize(400.dp, 800.dp)) { App() }
            }
        }
        compose.onNodeWithText(demoMessages.first().subject).performClick()
        compose.onNodeWithText("Lake weekend.pdf").performScrollTo().assertHasClickAction().performClick()
        compose.onNodeWithText(attachmentNotice).assertExists()
        compose.onNodeWithContentDescription(back).performClick()
        compose.onNodeWithText(demoMessages.first().subject).assertIsDisplayed()
        compose.onNodeWithText("Lake weekend.pdf").assertHasClickAction()
    }
}
