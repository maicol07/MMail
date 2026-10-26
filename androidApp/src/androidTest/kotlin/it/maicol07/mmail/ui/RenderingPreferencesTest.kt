package it.maicol07.mmail.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import it.maicol07.mmail.data.RenderingOptions
import it.maicol07.mmail.data.RenderingPreferences
import it.maicol07.mmail.data.rememberRenderingPreferences
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RenderingPreferencesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun defaultsArePrivateAndChangesSurviveReload() {
        // Use the test APK's storage to keep the user's preferences untouched.
        val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().context) {
            override fun getApplicationContext(): Context = this
        }
        val storage = context.getSharedPreferences("message_rendering", Context.MODE_PRIVATE)
        storage.edit().clear().commit()
        try {
            var generation by mutableStateOf(0)
            lateinit var preferences: RenderingPreferences
            compose.setContent {
                CompositionLocalProvider(LocalContext provides context) {
                    key(generation) { preferences = rememberRenderingPreferences() }
                }
            }
            val enabled = RenderingOptions(images = true, javaScript = true, externalStyles = true)
            compose.runOnIdle {
                assertEquals(RenderingOptions(), preferences.options)
                preferences.update(enabled)
                generation++
            }
            compose.runOnIdle { assertEquals(enabled, preferences.options) }
        } finally {
            storage.edit().clear().commit()
        }
    }
}
