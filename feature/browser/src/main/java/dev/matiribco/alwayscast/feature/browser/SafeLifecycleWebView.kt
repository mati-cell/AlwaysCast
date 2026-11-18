@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package dev.matiribco.alwayscast.feature.browser

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafeLifecycleWebView(
    url: String,
    callbacks: BrowserWebViewCallbacks,
    modifier: Modifier = Modifier,
    onWebViewCreated: ((WebView) -> Unit)? = null,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val shield = remember { AntiPopupShield() }
    val sniffer = remember { MediaSniffer() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setupWebViewSettings(this)
                setupBridgeAndClients(this, shield, sniffer, callbacks)
                webViewRef = this
                onWebViewCreated?.invoke(this)
                if (url.isNotBlank()) {
                    loadUrl(url)
                }
            }
        },
        update = { webView ->
            if (url.isNotBlank() && webView.url != url) {
                webView.loadUrl(url)
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { cleanUpWebView(it) }
            webViewRef = null
        }
    }
}

private fun setupWebViewSettings(webView: WebView) {
    webView.layoutParams =
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        mediaPlaybackRequiresUserGesture = false
        setSupportMultipleWindows(false)
    }
}

private fun setupBridgeAndClients(
    webView: WebView,
    shield: AntiPopupShield,
    sniffer: MediaSniffer,
    callbacks: BrowserWebViewCallbacks,
) {
    val bridge =
        MediaDetectionBridge(
            sniffer = sniffer,
            onCandidateDetected = callbacks.onCandidateDetected,
        )
    webView.addJavascriptInterface(bridge, MediaDetectionBridge.JAVASCRIPT_INTERFACE_NAME)
    webView.webChromeClient =
        object : WebChromeClient() {
            override fun onProgressChanged(
                view: WebView?,
                newProgress: Int,
            ) {
                super.onProgressChanged(view, newProgress)
                callbacks.onProgressChanged(newProgress)
            }
        }
    webView.webViewClient =
        SniffingWebViewClient(
            shield = shield,
            sniffer = sniffer,
            callbacks = callbacks,
        )
}

private fun cleanUpWebView(webView: WebView) {
    (webView.parent as? ViewGroup)?.removeView(webView)
    webView.stopLoading()
    webView.webChromeClient = null
    webView.webViewClient = WebViewClient()
    webView.removeJavascriptInterface(MediaDetectionBridge.JAVASCRIPT_INTERFACE_NAME)
    webView.clearHistory()
    webView.removeAllViews()
    webView.destroy()
}
