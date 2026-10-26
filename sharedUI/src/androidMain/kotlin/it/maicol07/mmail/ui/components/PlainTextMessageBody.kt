package it.maicol07.mmail.ui.components

import android.text.SpannableString
import android.text.style.URLSpan
import android.text.util.Linkify
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import it.maicol07.mmail.resources.Res
import it.maicol07.mmail.resources.error_open_link
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun PlainTextMessageBody(text: String, modifier: Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val linkError = stringResource(Res.string.error_open_link)
    val styles = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    val annotated = remember(text, styles, uriHandler, context, linkError) {
        val linked = SpannableString(text)
        Linkify.addLinks(linked, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
        buildAnnotatedString {
            append(text)
            for (span in linked.getSpans(0, linked.length, URLSpan::class.java)) {
                val link = LinkAnnotation.Url(span.url, styles) {
                    try {
                        uriHandler.openUri(span.url)
                    } catch (_: IllegalArgumentException) {
                        Toast.makeText(context, linkError, Toast.LENGTH_SHORT).show()
                    }
                }
                addLink(link, linked.getSpanStart(span), linked.getSpanEnd(span))
            }
        }
    }
    Text(annotated, modifier = modifier, style = MaterialTheme.typography.bodyLarge)
}
