package it.maicol07.mmail.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import it.maicol07.mmail.data.RenderingOptions

@Composable
expect fun HtmlMessageBody(
    html: String, modifier: Modifier, options: RenderingOptions = RenderingOptions(), onReady: () -> Unit = {},
)
