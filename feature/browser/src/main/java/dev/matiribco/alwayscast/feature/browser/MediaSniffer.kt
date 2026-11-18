package dev.matiribco.alwayscast.feature.browser

import dev.matiribco.alwayscast.core.model.CandidateSource
import dev.matiribco.alwayscast.core.model.MediaCandidate
import java.util.Locale

class MediaSniffer {
    fun sniff(
        url: String,
        headers: Map<String, String> = emptyMap(),
        contentType: String? = null,
    ): MediaCandidate? {
        val lowerUrl = url.lowercase(Locale.ROOT)
        val resolvedMime = resolveMimeType(lowerUrl, contentType) ?: return null

        return MediaCandidate(
            url = url,
            headers = headers,
            contentType = resolvedMime,
            source = CandidateSource.BROWSER,
        )
    }

    private fun resolveMimeType(
        lowerUrl: String,
        contentType: String?,
    ): String? =
        when {
            lowerUrl.contains(".m3u8") -> MIME_HLS
            lowerUrl.contains(".mp4") -> MIME_MP4
            lowerUrl.contains(".webm") -> MIME_WEBM
            lowerUrl.contains(".mpd") -> MIME_DASH
            contentType?.startsWith("video/") == true -> contentType
            contentType?.contains("mpegurl", ignoreCase = true) == true -> MIME_HLS
            else -> null
        }

    companion object {
        const val MIME_HLS = "application/x-mpegURL"
        const val MIME_MP4 = "video/mp4"
        const val MIME_WEBM = "video/webm"
        const val MIME_DASH = "application/dash+xml"
    }
}
