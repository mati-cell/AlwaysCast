package dev.matiribco.alwayscast.core.network

import dev.matiribco.alwayscast.core.model.CandidateSource
import dev.matiribco.alwayscast.core.model.MediaCandidate
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.regex.Pattern

internal object Html5MediaParser {
    private const val HEADER_REFERER = "Referer"
    private const val MIME_HLS = "application/x-mpegURL"
    private const val MIME_MP4 = "video/mp4"

    private val HTML5_SOURCE_PATTERN: Pattern =
        Pattern.compile(
            "<source[^>]+src=[\"']([^\"']+)[\"'](?:[^>]*type=[\"']([^\"']+)[\"'])?",
            Pattern.CASE_INSENSITIVE,
        )
    private val HTML5_VIDEO_PATTERN: Pattern =
        Pattern.compile("<video[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)

    fun parse(
        pageUrl: String,
        html: String,
    ): List<MediaCandidate> {
        val candidates = mutableListOf<MediaCandidate>()
        val base = pageUrl.toHttpUrlOrNull()
        extractSourceTags(base, pageUrl, html, candidates)
        extractVideoTags(base, pageUrl, html, candidates)
        return candidates
    }

    private fun extractSourceTags(
        base: HttpUrl?,
        pageUrl: String,
        html: String,
        candidates: MutableList<MediaCandidate>,
    ) {
        val matcher = HTML5_SOURCE_PATTERN.matcher(html)
        while (matcher.find()) {
            val src = matcher.group(1) ?: continue
            val type = matcher.group(2)
            val absoluteUrl = resolveUrl(base, src)
            candidates.add(
                MediaCandidate(
                    url = absoluteUrl,
                    headers = mapOf(HEADER_REFERER to pageUrl),
                    contentType = type ?: guessType(absoluteUrl),
                    source = CandidateSource.BROWSER,
                    label = "Stream (${type ?: "video"})",
                ),
            )
        }
    }

    private fun extractVideoTags(
        base: HttpUrl?,
        pageUrl: String,
        html: String,
        candidates: MutableList<MediaCandidate>,
    ) {
        val matcher = HTML5_VIDEO_PATTERN.matcher(html)
        while (matcher.find()) {
            val src = matcher.group(1) ?: continue
            val absoluteUrl = resolveUrl(base, src)
            candidates.add(
                MediaCandidate(
                    url = absoluteUrl,
                    headers = mapOf(HEADER_REFERER to pageUrl),
                    contentType = guessType(absoluteUrl),
                    source = CandidateSource.BROWSER,
                    label = "Video HTML5",
                ),
            )
        }
    }

    private fun resolveUrl(
        base: HttpUrl?,
        src: String,
    ): String = base?.resolve(src)?.toString() ?: src

    private fun guessType(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".m3u8") -> MIME_HLS
            lower.contains(".mp4") -> MIME_MP4
            lower.contains(".webm") -> "video/webm"
            lower.contains(".mkv") -> "video/x-matroska"
            else -> MIME_MP4
        }
    }
}
