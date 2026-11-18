package dev.matiribco.alwayscast.feature.browser

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class SniffingWebViewClient(
    private val shield: AntiPopupShield,
    private val sniffer: MediaSniffer,
    private val callbacks: BrowserWebViewCallbacks,
    private val defaultUserAgent: String = "",
) : WebViewClient() {
    @Volatile
    private var currentPageUrl: String? = null

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
            val headers = request.requestHeaders?.toMutableMap() ?: mutableMapOf()
            val pageUrl = currentPageUrl
            if (!headers.containsKey("Referer") && pageUrl != null) {
                headers["Referer"] = pageUrl
            }
            if (!headers.containsKey("User-Agent") && defaultUserAgent.isNotBlank()) {
                headers["User-Agent"] = defaultUserAgent
            }
            val candidate = sniffer.sniff(url = requestUrl, headers = headers)
            if (candidate != null) {
                postToMain {
                    callbacks.onCandidateDetected(candidate)
                }
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
            currentPageUrl = url
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
            currentPageUrl = url
            callbacks.onPageFinished(url, view?.title)
            callbacks.onHistoryChanged(view?.canGoBack() == true, view?.canGoForward() == true)
            injectDefensiveScripts(view)
        }
    }

    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?,
    ): Boolean = true

    private fun postToMain(action: () -> Unit) {
        val dispatched =
            runCatching {
                Handler(Looper.getMainLooper()).post(action)
            }.getOrDefault(false)

        if (!dispatched) {
            action()
        }
    }

    private fun injectDefensiveScripts(view: WebView?) {
        view?.evaluateJavascript(shield.getShieldScript(), null)
        view?.evaluateJavascript(MediaDetectionBridge.MEDIA_HOOK_SCRIPT, null)
    }
}
