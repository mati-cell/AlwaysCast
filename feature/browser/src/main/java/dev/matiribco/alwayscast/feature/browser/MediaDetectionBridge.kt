package dev.matiribco.alwayscast.feature.browser

import android.webkit.JavascriptInterface
import dev.matiribco.alwayscast.core.model.MediaCandidate

class MediaDetectionBridge(
    private val sniffer: MediaSniffer,
    private val onCandidateDetected: (MediaCandidate) -> Unit,
) {
    @JavascriptInterface
    fun onMediaDetected(
        url: String,
        mimeType: String?,
    ) {
        val candidate = sniffer.sniff(url = url, contentType = mimeType)
        if (candidate != null) {
            onCandidateDetected(candidate)
        }
    }

    companion object {
        const val JAVASCRIPT_INTERFACE_NAME = "AlwaysCastBridge"

        const val MEDIA_HOOK_SCRIPT = """
(function() {
    function notifyMedia(src, type) {
        if (src && window.AlwaysCastBridge && window.AlwaysCastBridge.onMediaDetected) {
            window.AlwaysCastBridge.onMediaDetected(src, type || '');
        }
    }

    var originalPlay = HTMLMediaElement.prototype.play;
    HTMLMediaElement.prototype.play = function() {
        if (this.currentSrc) {
            notifyMedia(this.currentSrc, this.type);
        } else if (this.src) {
            notifyMedia(this.src, this.type);
        }
        return originalPlay.apply(this, arguments);
    };

    var videos = document.getElementsByTagName('video');
    for (var i = 0; i < videos.length; i++) {
        var v = videos[i];
        if (v.src) notifyMedia(v.src, v.type);
        var sources = v.getElementsByTagName('source');
        for (var j = 0; j < sources.length; j++) {
            if (sources[j].src) notifyMedia(sources[j].src, sources[j].type);
        }
    }
})();
"""
    }
}
