package dev.matiribco.alwayscast.feature.browser

import dev.matiribco.alwayscast.core.model.CandidateSource
import dev.matiribco.alwayscast.core.model.MediaCandidate
import java.net.URI
import java.util.Locale

class MediaSniffer {
    fun sniff(
        url: String,
        headers: Map<String, String> = emptyMap(),
        contentType: String? = null,
    ): MediaCandidate? {
        val path = extractPath(url)
        val resolvedMime = resolveMimeType(path, contentType) ?: return null

        return MediaCandidate(
            url = url,
            headers = headers,
            contentType = resolvedMime,
            source = CandidateSource.BROWSER,
        )
    }

    private fun extractPath(url: String): String =
        try {
            val uri = URI(url)
            uri.path?.lowercase(Locale.ROOT) ?: ""
        } catch (_: Exception) {
            val noQuery = url.substringBefore('?').substringBefore('#')
            noQuery.lowercase(Locale.ROOT)
        }

    private fun resolveMimeType(
        path: String,
        contentType: String?,
    ): String? =
        when {
            path.endsWith(".m3u8") -> MIME_HLS
            path.endsWith(".mp4") -> MIME_MP4
            path.endsWith(".mkv") -> MIME_MKV
            path.endsWith(".webm") -> MIME_WEBM
            contentType?.startsWith("video/") == true -> contentType
            contentType?.contains("mpegurl", ignoreCase = true) == true -> MIME_HLS
            else -> null
        }

    companion object {
        const val MIME_HLS = "application/x-mpegURL"
        const val MIME_MP4 = "video/mp4"
        const val MIME_MKV = "video/x-matroska"
        const val MIME_WEBM = "video/webm"
    }
}
