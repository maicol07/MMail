package it.maicol07.mmail.ui

import android.webkit.WebView
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import it.maicol07.mmail.ui.components.HtmlMessageBody
import it.maicol07.mmail.ui.components.MessageBody
import it.maicol07.mmail.data.RenderingOptions
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

class HtmlMessageBodyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
        else -> null
    }

    @Test
    fun switchingFromPlainTextToHtmlPreservesTheParentScrollPosition() {
        val scroll = ScrollState(0)
        var plainText by mutableStateOf(true)
        compose.setContent {
            Box(Modifier.requiredSize(400.dp, 800.dp)) {
                Column(Modifier.verticalScroll(scroll)) {
                    Spacer(Modifier.height(900.dp))
                    Text("Message controls")
                    MessageBody("Email paragraph\n".repeat(100), "<p>Email paragraph</p>".repeat(60), plainText, RenderingOptions(), Modifier.fillMaxWidth().testTag("body-transition"))
                }
            }
        }
        val previous = compose.runOnIdle {
            runBlocking { scroll.scrollTo((800 * compose.activity.resources.displayMetrics.density).roundToInt()) }
            plainText = false
            scroll.value
        }
        compose.waitUntil(10_000) {
            compose.runOnIdle { findWebView(compose.activity.window.decorView)?.let { it.progress == 100 && it.contentHeight > 0 } == true }
        }
        compose.waitUntil(10_000) {
            val nativeHeight = compose.runOnIdle { findWebView(compose.activity.window.decorView)!!.height / compose.activity.resources.displayMetrics.density }
            val bounds = compose.onNodeWithTag("body-transition").getUnclippedBoundsInRoot()
            kotlin.math.abs(bounds.bottom.value - bounds.top.value - nativeHeight) < 1f
        }
        compose.runOnIdle { assertEquals(previous, scroll.value) }
    }

    @Test
    fun changingRenderingOptionsPreservesTheParentScrollPosition() {
        val scroll = ScrollState(0)
        var options by mutableStateOf(RenderingOptions())
        val image = Base64.encodeToString("""<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect width="200" height="200" fill="red"/></svg>""".toByteArray(), Base64.NO_WRAP)
        val html = "<p>Email paragraph</p>".repeat(100) + "<img src=\"data:image/svg+xml;base64,$image\">"
        compose.setContent {
            Box(Modifier.requiredSize(400.dp, 800.dp)) {
                Column(Modifier.verticalScroll(scroll)) {
                    Spacer(Modifier.height(900.dp))
                    Text("Message controls")
                    HtmlMessageBody(html, Modifier.fillMaxWidth(), options)
                }
            }
        }
        compose.waitUntil(10_000) { compose.runOnIdle { findWebView(compose.activity.window.decorView)!!.contentHeight > 0 } }
        val previousHeight = compose.runOnIdle { findWebView(compose.activity.window.decorView)!!.contentHeight }
        val previous = compose.runOnIdle {
            val target = (800 * compose.activity.resources.displayMetrics.density).roundToInt()
            runBlocking { scroll.scrollTo(target) }
            options = RenderingOptions(images = true)
            scroll.value
        }
        compose.waitUntil(10_000) {
            compose.runOnIdle {
                findWebView(compose.activity.window.decorView)?.let { it.contentHeight > previousHeight } == true
            }
        }
        compose.runOnIdle { assertEquals(previous, scroll.value) }
    }

    @Test
    fun rendersLongHtmlWithoutEnablingNetworkScriptsOrLocalAccess() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HtmlMessageBody("<h1>HTML email</h1>" + "<p>Readable paragraph</p>".repeat(60), Modifier.fillMaxWidth())
                    Text("End of message", Modifier.testTag("message-end"))
                }
            }
        }
        var webView: WebView? = null
        compose.runOnIdle {
            val view = findWebView(compose.activity.window.decorView)!!
            webView = view
            with(view.settings) {
                assertFalse(javaScriptEnabled)
                assertFalse(allowFileAccess)
                assertFalse(allowContentAccess)
                assertFalse(domStorageEnabled)
                assertTrue(blockNetworkLoads)
                assertEquals(150, textZoom)
            }
        }
        compose.waitUntil(10_000) { compose.runOnIdle { webView!!.contentHeight > 0 } }
        compose.runOnIdle {
            assertTrue("HTML has content but occupies no screen space: height=${webView!!.height}, contentHeight=${webView!!.contentHeight}", webView!!.height > 0)
        }
        compose.onNodeWithTag("message-end").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun enablingImagesAndScriptsWorksAndRevokingPermissionsReplacesTheDocument() {
        var options by mutableStateOf(RenderingOptions(images = true, javaScript = true, externalStyles = true))
        val html = """<p>Image and script test</p>
            <img onload="document.title='image loaded';setInterval(function(){document.title='image loaded'},50)" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+X2ioAAAAASUVORK5CYII=">
            <script>document.title='script loaded'</script>"""
        compose.setContent { HtmlMessageBody(html, Modifier.fillMaxWidth(), options) }
        compose.waitUntil(10_000) {
            compose.runOnIdle { findWebView(compose.activity.window.decorView)?.title == "image loaded" }
        }
        val enabled = compose.runOnIdle {
            findWebView(compose.activity.window.decorView)!!.also { view ->
                assertTrue(view.settings.javaScriptEnabled)
                assertFalse(view.settings.blockNetworkLoads)
                assertFalse(view.settings.blockNetworkImage)
                assertTrue(view.settings.loadsImagesAutomatically)
                options = RenderingOptions()
            }
        }
        compose.waitUntil(10_000) {
            compose.runOnIdle { findWebView(compose.activity.window.decorView)?.let { it.progress == 100 && it.title != "image loaded" } == true }
        }
        compose.runOnIdle {
            val disabled = findWebView(compose.activity.window.decorView)!!
            assertSame(enabled, disabled)
            assertFalse(disabled.settings.javaScriptEnabled)
            assertTrue(disabled.settings.blockNetworkLoads)
            assertTrue(disabled.settings.blockNetworkImage)
            assertFalse(disabled.settings.allowFileAccess)
            assertFalse(disabled.settings.allowContentAccess)
        }
    }
}
