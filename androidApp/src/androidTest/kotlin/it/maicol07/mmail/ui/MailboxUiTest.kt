package it.maicol07.mmail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import it.maicol07.mmail.App
import it.maicol07.mmail.data.demoMessages
import org.junit.Rule
import org.junit.Test

class MailboxUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun filterSheetAndUnreadNavigationKeepTheOpenedMessageVisible() {
        compose.setContent { Box(Modifier.requiredSize(400.dp, 800.dp)) { App() } }
        compose.onNodeWithText("Unread").assertDoesNotExist()
        compose.onNodeWithContentDescription("Filters").performClick()
        compose.onNodeWithText("Unread").performClick()
        compose.onNodeWithText("Show messages").performClick()
        compose.onNodeWithContentDescription("Filters: Unread").assertExists()

        compose.onNodeWithText(demoMessages.first().subject).performClick()
        compose.onNodeWithContentDescription("Back to messages").assertIsDisplayed()
        compose.onNodeWithText(demoMessages.first().body).assertExists()
        compose.onNodeWithContentDescription("Back to messages").performClick()
        compose.onNodeWithText(demoMessages.first().subject).assertDoesNotExist()
    }

    @Test
    fun sharedTransitionRoundTripAndAttachmentChipRemainInteractive() {
        compose.setContent { Box(Modifier.requiredSize(400.dp, 800.dp)) { App() } }
        compose.onNodeWithText(demoMessages.first().subject).performClick()
        compose.onNodeWithText("Lake weekend.pdf").performScrollTo().assertHasClickAction().performClick()
        compose.onNodeWithText("Attachments are examples and contain no actual files.").assertExists()
        compose.onNodeWithContentDescription("Back to messages").performClick()
        compose.onNodeWithText(demoMessages.first().subject).assertIsDisplayed()
        compose.onNodeWithText("Lake weekend.pdf").assertHasClickAction()
    }
}
