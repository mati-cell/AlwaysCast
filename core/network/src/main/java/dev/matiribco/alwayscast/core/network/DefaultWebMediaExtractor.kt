@file:Suppress("TooGenericExceptionCaught")

package dev.matiribco.alwayscast.core.network

import dev.matiribco.alwayscast.core.model.CandidateSource
import dev.matiribco.alwayscast.core.model.MediaCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class DefaultWebMediaExtractor(
    private val client: OkHttpClient,
) : WebMediaExtractor {
    override suspend fun extract(
        url: String,
        headers: Map<String, String>,
    ): List<MediaCandidate> =
        withContext(Dispatchers.IO) {
            try {
                val request = buildGetRequest(url, headers)
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()

                    val contentType = response.header(HEADER_CONTENT_TYPE).orEmpty()
                    if (isDirectMedia(contentType, url)) {
                        return@withContext listOf(createDirectCandidate(url, headers, contentType))
                    }

                    val bodyString = response.body?.string().orEmpty()
                    if (bodyString.isBlank()) return@withContext emptyList()

                    extractFromHtml(url, bodyString)
                }
            } catch (
                @Suppress("SwallowedException") exception: Exception,
            ) {
                emptyList()
            }
        }

    private fun extractFromHtml(
        url: String,
        html: String,
    ): List<MediaCandidate> {
        val okCandidates = OkRuParser.parse(url, html)
        val candidates = okCandidates.ifEmpty { Html5MediaParser.parse(url, html) }
        return candidates.distinctBy { it.url }
    }

    private fun createDirectCandidate(
        url: String,
        headers: Map<String, String>,
        contentType: String,
    ): MediaCandidate =
        MediaCandidate(
            url = url,
            headers = headers,
            contentType = contentType.substringBefore(PARAM_SEPARATOR).trim(),
            source = CandidateSource.PASTED,
            label = DIRECT_MEDIA_LABEL,
        )

    private fun isDirectMedia(
        contentType: String,
        url: String,
    ): Boolean {
        val lowerType = contentType.lowercase()
        val lowerUrl = url.lowercase()
        return lowerType.startsWith(MIME_VIDEO_PREFIX) ||
            lowerType.contains(MIME_M3U8_KEYWORD) ||
            DIRECT_EXTENSIONS.any { lowerUrl.endsWith(it) }
    }

    private fun buildGetRequest(
        url: String,
        headers: Map<String, String>,
    ): Request {
        val builder =
            Request
                .Builder()
                .url(url)
                .header(HEADER_USER_AGENT, USER_AGENT_MOBILE)
                .header(HEADER_ACCEPT, ACCEPT_HEADER_VALUE)
        headers.forEach { (key, value) -> builder.header(key, value) }
        return builder.build()
    }

    private companion object {
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val HEADER_USER_AGENT = "User-Agent"
        const val HEADER_ACCEPT = "Accept"
        const val PARAM_SEPARATOR = ';'
        const val MIME_VIDEO_PREFIX = "video/"
        const val MIME_M3U8_KEYWORD = "mpegurl"
        const val DIRECT_MEDIA_LABEL = "Video Directo"
        const val USER_AGENT_MOBILE =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko)" +
                " Chrome/120.0.0.0 Mobile Safari/537.36"
        const val ACCEPT_HEADER_VALUE =
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"

        val DIRECT_EXTENSIONS = listOf(".mp4", ".m3u8", ".webm", ".mkv")
    }
}
