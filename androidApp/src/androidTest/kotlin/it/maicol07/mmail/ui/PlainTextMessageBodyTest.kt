package it.maicol07.mmail.ui

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.LinkAnnotation
import it.maicol07.mmail.ui.components.PlainTextMessageBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlainTextMessageBodyTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun recognizesEveryWebAndEmailLinkWithoutChangingPlainText() {
        val change = "https://www.pcgamingwiki.com/w/index.php?title=Final_Fantasy_Resonance&diff=next&oldid=1819957"
        val body = "Caffè\n\nView this change: <$change>\nVisit www.example.com or email support@example.com.\n<b>Plain text</b>"
        compose.setContent {
            MaterialTheme {
                SelectionContainer { PlainTextMessageBody(body, Modifier.testTag("body")) }
            }
        }
        val annotated = compose.onNodeWithTag("body").fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertEquals(body, annotated.text)
        val links = annotated.getLinkAnnotations(0, annotated.length)
        assertEquals(listOf(change, "http://www.example.com", "mailto:support@example.com"), links.map { (it.item as LinkAnnotation.Url).url })
        assertEquals(listOf(change, "www.example.com", "support@example.com"), links.map { body.substring(it.start, it.end) })
    }

    @Test
    fun clickingALinkInSelectableTextOpensTheExactUrl() {
        val url = "https://www.pcgamingwiki.com/w/index.php?title=Example&diff=next&oldid=123"
        var opened: String? = null
        val handler = object : UriHandler {
            override fun openUri(uri: String) { opened = uri }
        }
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                MaterialTheme {
                    SelectionContainer { PlainTextMessageBody("View changes: <$url>", Modifier) }
                }
            }
        }
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)[0].performClick()
        compose.runOnIdle { assertEquals(url, opened) }
    }
}
