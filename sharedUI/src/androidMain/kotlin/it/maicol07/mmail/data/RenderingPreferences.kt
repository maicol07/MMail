package it.maicol07.mmail.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal fun loadRenderingPreferences(context: Context): RenderingPreferences {
    val storage = context.getSharedPreferences("message_rendering", Context.MODE_PRIVATE)
    return RenderingPreferences(
        RenderingOptions(
            images = storage.getBoolean("images", false),
            javaScript = storage.getBoolean("javascript", false),
            externalStyles = storage.getBoolean("external_styles", false),
        ),
        save = { value ->
            storage.edit().putBoolean("images", value.images).putBoolean("javascript", value.javaScript)
                .putBoolean("external_styles", value.externalStyles).apply()
        },
    )
}

@Composable
actual fun rememberRenderingPreferences(): RenderingPreferences {
    val context = LocalContext.current.applicationContext
    return remember(context) { loadRenderingPreferences(context) }
}
