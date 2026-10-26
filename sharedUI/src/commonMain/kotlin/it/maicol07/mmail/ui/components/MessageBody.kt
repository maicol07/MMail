package it.maicol07.mmail.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.maicol07.mmail.data.RenderingOptions

@Composable
fun MessageBody(text: String, html: String?, plainText: Boolean, options: RenderingOptions, modifier: Modifier) {
    var previousHeight by remember(text, html) { mutableIntStateOf(0) }
    var htmlReady by remember(html, plainText) { mutableStateOf(false) }
    val waitingForHtml = html != null && !plainText && !htmlReady
    val minimumHeight = with(LocalDensity.current) { if (waitingForHtml) previousHeight.toDp() else 0.dp }
    Box(modifier.heightIn(min = minimumHeight).onSizeChanged { previousHeight = it.height }) {
        if (html == null || plainText) {
            SelectionContainer { Text(text, style = MaterialTheme.typography.bodyLarge) }
        } else {
            HtmlMessageBody(html, Modifier.fillMaxWidth().testTag("message-html"), options, onReady = { htmlReady = true })
        }
    }
}
