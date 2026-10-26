package it.maicol07.mmail.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class RenderingOptions(
    val images: Boolean = false,
    val javaScript: Boolean = false,
    val externalStyles: Boolean = false,
)

class RenderingPreferences(initial: RenderingOptions, private val save: (RenderingOptions) -> Unit) {
    var options: RenderingOptions by mutableStateOf(initial)
        private set

    fun update(value: RenderingOptions) {
        save(value)
        options = value
    }
}

@Composable
expect fun rememberRenderingPreferences(): RenderingPreferences
