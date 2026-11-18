package dev.matiribco.alwayscast.core.network

import dev.matiribco.alwayscast.core.model.CandidateSource
import dev.matiribco.alwayscast.core.model.MediaCandidate
import org.json.JSONObject
import java.util.regex.Pattern

internal object OkRuParser {
    private const val HEADER_REFERER = "Referer"
    private const val MIME_HLS = "application/x-mpegURL"
    private const val MIME_MP4 = "video/mp4"
    private const val LABEL_HLS_AUTO = "HLS Adaptativo (.m3u8)"
    private const val KEY_FLASHVARS = "flashvars"
    private const val KEY_METADATA = "metadata"
    private const val KEY_HLS_MANIFEST = "hlsManifestUrl"
    private const val KEY_VIDEOS = "videos"
    private const val KEY_URL = "url"
    private const val KEY_NAME = "name"

    private val OK_OPTIONS_PATTERN: Pattern =
        Pattern.compile("data-options=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)

    fun parse(
        pageUrl: String,
        html: String,
    ): List<MediaCandidate> {
        val match = OK_OPTIONS_PATTERN.matcher(html)
        val rawEncoded = if (match.find()) match.group(1) else null
        return if (!rawEncoded.isNullOrBlank()) {
            parseJsonMetadata(pageUrl, unescapeHtml(rawEncoded))
        } else {
            emptyList()
        }
    }

    private fun parseJsonMetadata(
        pageUrl: String,
        rawJson: String,
    ): List<MediaCandidate> =
        try {
            val root = JSONObject(rawJson)
            val metadataObj = extractMetadataObject(root)
            val candidates = mutableListOf<MediaCandidate>()
            if (metadataObj != null) {
                addHlsCandidate(metadataObj, pageUrl, candidates)
                addVideoCandidates(metadataObj, pageUrl, candidates)
            }
            candidates
        } catch (
            @Suppress("SwallowedException", "TooGenericExceptionCaught") exception: Exception,
        ) {
            emptyList()
        }

    private fun extractMetadataObject(root: JSONObject): JSONObject? {
        val flashvars = root.optJSONObject(KEY_FLASHVARS) ?: return null
        val directObj = flashvars.optJSONObject(KEY_METADATA)
        val metaStr = if (directObj == null) flashvars.optString(KEY_METADATA) else null
        return directObj ?: if (!metaStr.isNullOrBlank()) JSONObject(metaStr) else null
    }

    private fun addHlsCandidate(
        metadataObj: JSONObject,
        pageUrl: String,
        candidates: MutableList<MediaCandidate>,
    ) {
        val hlsUrl = metadataObj.optString(KEY_HLS_MANIFEST)
        if (hlsUrl.isNotBlank()) {
            candidates.add(
                MediaCandidate(
                    url = hlsUrl,
                    headers = mapOf(HEADER_REFERER to pageUrl),
                    contentType = MIME_HLS,
                    source = CandidateSource.BROWSER,
                    label = LABEL_HLS_AUTO,
                ),
            )
        }
    }

    private fun addVideoCandidates(
        metadataObj: JSONObject,
        pageUrl: String,
        candidates: MutableList<MediaCandidate>,
    ) {
        val videosArray = metadataObj.optJSONArray(KEY_VIDEOS) ?: return
        for (i in 0 until videosArray.length()) {
            val vObj = videosArray.optJSONObject(i) ?: continue
            val vUrl = vObj.optString(KEY_URL)
            val vName = vObj.optString(KEY_NAME)
            if (vUrl.isNotBlank()) {
                candidates.add(
                    MediaCandidate(
                        url = vUrl,
                        headers = mapOf(HEADER_REFERER to pageUrl),
                        contentType = MIME_MP4,
                        source = CandidateSource.BROWSER,
                        label = formatQualityLabel(vName),
                    ),
                )
            }
        }
    }

    private fun formatQualityLabel(name: String): String =
        when (name.lowercase()) {
            "hd" -> "MP4 - Alta Definición (HD)"
            "sd" -> "MP4 - Definición Estándar (SD)"
            "low" -> "MP4 - Baja Calidad (360p)"
            "lowest" -> "MP4 - Mínima Calidad (240p)"
            "mobile" -> "MP4 - Móvil (144p)"
            else -> "MP4 - ${name.uppercase()}"
        }

    private fun unescapeHtml(text: String): String =
        text
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
}
