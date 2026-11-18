package dev.matiribco.alwayscast.feature.browser

import android.graphics.Bitmap
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class SniffingWebViewClient(
    private val shield: AntiPopupShield,
    private val sniffer: MediaSniffer,
    private val callbacks: BrowserWebViewCallbacks,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        return shield.shouldBlockUrl(url)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val requestUrl = request?.url?.toString()
        if (requestUrl != null) {
            val headers = request.requestHeaders ?: emptyMap()
            val candidate = sniffer.sniff(url = requestUrl, headers = headers)
            if (candidate != null) {
                callbacks.onCandidateDetected(candidate)
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?,
    ) {
        super.onPageStarted(view, url, favicon)
        if (url != null) {
            callbacks.onPageStarted(url)
            injectDefensiveScripts(view)
        }
    }

    override fun onPageFinished(
        view: WebView?,
        url: String?,
    ) {
        super.onPageFinished(view, url)
        if (url != null) {
            callbacks.onPageFinished(url, view?.title)
            injectDefensiveScripts(view)
        }
    }

    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?,
    ): Boolean = true

    private fun injectDefensiveScripts(view: WebView?) {
        view?.evaluateJavascript(shield.getShieldScript(), null)
        view?.evaluateJavascript(MediaDetectionBridge.MEDIA_HOOK_SCRIPT, null)
    }
}
