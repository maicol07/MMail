package it.maicol07.mmail.ui

import android.graphics.Bitmap
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.intl.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import it.maicol07.mmail.App
import it.maicol07.mmail.data.RenderingOptions
import it.maicol07.mmail.data.RenderingPreferences
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RenderingFrameTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun switchingToHtmlKeepsTheMessageHeaderVisibleInEveryFrame() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            compose.runOnUiThread {
                compose.activity.enableEdgeToEdge()
                compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (Build.VERSION.SDK_INT >= 27) {
                    compose.activity.setShowWhenLocked(true)
                    compose.activity.setTurnScreenOn(true)
                }
            }
            val provider = TestMailProvider().apply {
                fullBody = "Notification paragraph\nhttps://www.pcgamingwiki.com/w/index.php?title=Example&diff=next\n".repeat(50)
                htmlBody = "<html><body><pre style=\"white-space:pre-wrap;word-wrap:break-word\">$fullBody</pre></body></html>"
            }
            val preferences = RenderingPreferences(RenderingOptions()) { }
            compose.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.Locales(LocaleList("en"))) { App(provider, preferences) }
            }
            compose.onNodeWithText(provider.message.subject).performClick()
            compose.onNodeWithText("Show plain text").performScrollTo().performClick()
            val toggle = compose.onNodeWithText("Show formatted message").performScrollTo()
            val density = compose.activity.resources.displayMetrics.density
            val top = (compose.onNodeWithContentDescription("Back to messages").getUnclippedBoundsInRoot().top.value * density).toInt()
            val bottom = ((toggle.getUnclippedBoundsInRoot().top.value - 16) * density).toInt()
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val before = requireNotNull(automation.takeScreenshot())
            val baseline = Bitmap.createBitmap(before, 0, top, before.width, bottom - top)
            before.recycle()
            val expected = IntArray(baseline.width * baseline.height)
            baseline.getPixels(expected, 0, baseline.width, 0, 0, baseline.width, baseline.height)
            compose.mainClock.autoAdvance = false
            toggle.performClick()
            val changedFrames = mutableListOf<Int>()
            repeat(30) { frame ->
                compose.mainClock.advanceTimeByFrame()
                Thread.sleep(16)
                val screenshot = requireNotNull(automation.takeScreenshot())
                val pixels = IntArray(expected.size)
                screenshot.getPixels(pixels, 0, baseline.width, 0, top, baseline.width, baseline.height)
                val changed = pixels.indices.count { pixels[it] != expected[it] }
                if (changed > expected.size / 100) {
                    changedFrames += frame
                    val output = File(compose.activity.getExternalFilesDir(null), "render-frame-$frame.png")
                    output.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                screenshot.recycle()
            }
            compose.mainClock.autoAdvance = true
            baseline.recycle()
            assertEquals("Header changed in frames $changedFrames", emptyList<Int>(), changedFrames)
            compose.runOnIdle {
                assertEquals(1, provider.mailboxLoads)
                assertEquals(1, provider.messageLoads)
            }
        } finally {
            compose.mainClock.autoAdvance = true
            Locale.setDefault(originalLocale)
        }
    }
}
