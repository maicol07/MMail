package it.maicol07.mmail.ui.components

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.WebContent
import com.kevinnzou.web.WebViewState
import com.kevinnzou.web.WebView as ComposeWebView
import it.maicol07.mmail.data.RenderingOptions
import it.maicol07.mmail.resources.Res
import it.maicol07.mmail.resources.error_open_link
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
actual fun HtmlMessageBody(html: String, modifier: Modifier, options: RenderingOptions, onReady: () -> Unit) {
    val document = remember(html, options) { emailHtmlDocument(html, options) }
    val uriHandler = LocalUriHandler.current
    val linkError = stringResource(Res.string.error_open_link)
    val textZoom = (LocalDensity.current.fontScale * 100).roundToInt()
    key(html) {
        val contentReady by rememberUpdatedState(onReady)
        val content = remember(document) { WebContent.Data(document, baseUrl = "https://email.invalid/", mimeType = "text/html") }
        val state = remember { WebViewState(content) }
        var webView by remember { mutableStateOf<WebView?>(null) }
        SideEffect {
            webView?.let { view ->
                view.applyRenderingOptions(options, textZoom)
                if (state.content != content) {
                    // Navigating replaces the script context without collapsing the native view's layout.
                    view.stopLoading()
                    state.content = content
                }
            }
        }
        val client = remember {
            object : AccompanistWebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    view.postVisualStateCallback(0, object : WebView.VisualStateCallback() {
                        override fun onComplete(requestId: Long) {
                            view.requestLayout()
                            contentReady()
                        }
                    })
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.isForMainFrame && request.hasGesture() && request.url.scheme in listOf("https", "http", "mailto")) {
                        try {
                            uriHandler.openUri(request.url.toString())
                        } catch (_: IllegalArgumentException) {
                            Toast.makeText(view.context, linkError, Toast.LENGTH_SHORT).show()
                        }
                    }
                    return true
                }
            }
        }
        // The first native frames can otherwise paint over the message header.
        ComposeWebView(
            state = state, modifier = modifier.clipToBounds(), captureBackPresses = false, client = client,
            onCreated = { view ->
                CookieManager.getInstance().setAcceptThirdPartyCookies(view, false)
                view.settings.apply {
                    allowFileAccess = false
                    allowContentAccess = false
                    domStorageEnabled = false
                    setGeolocationEnabled(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                view.applyRenderingOptions(options, textZoom)
                webView = view
            },
            onDispose = { view -> view.stopLoading(); view.destroy() },
        )
    }
}

private fun WebView.applyRenderingOptions(options: RenderingOptions, textZoom: Int) {
    settings.apply {
        javaScriptEnabled = options.javaScript
        blockNetworkLoads = !options.images && !options.javaScript && !options.externalStyles
        blockNetworkImage = !options.images
        loadsImagesAutomatically = options.images
        this.textZoom = textZoom
    }
}
