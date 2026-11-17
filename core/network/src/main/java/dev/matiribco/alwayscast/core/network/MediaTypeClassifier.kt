package dev.matiribco.alwayscast.core.network

import dev.matiribco.alwayscast.core.model.UnsupportedReason
import dev.matiribco.alwayscast.core.model.Verdict
import java.util.Locale

interface MediaClassifier {
    fun classify(
        url: String,
        contentType: String?,
    ): Verdict
}

object MediaTypeClassifier : MediaClassifier {
    private const val BLOB_PREFIX = "blob:"
    private const val MEDIA_SOURCE = "mediasource"
    private const val HVC1 = "hvc1"
    private const val HEV1 = "hev1"
    private const val HEVC = "hevc"
    private const val AV01 = "av01"
    private const val DOT_AV1 = ".av1"
    private const val WIDEVINE = "widevine"
    private const val DRM_TODAY = "drmtoday"
    private const val PLAYREADY = "playready"
    private const val PARAMETER_SEPARATOR = ';'
    private const val QUERY_SEPARATOR = '?'
    private const val FRAGMENT_SEPARATOR = '#'
    private const val PATH_SEPARATOR = '/'
    private const val EXTENSION_SEPARATOR = '.'
    private const val VIDEO_PREFIX = "video/"
    private const val AUDIO_PREFIX = "audio/"
    private const val HTML = "html"
    private const val TEXT_PLAIN = "text/plain"
    private const val EXT_HTML = "html"
    private const val EXT_HTM = "htm"
    private const val EXT_PHP = "php"
    private const val EXT_ASP = "asp"
    private const val EXT_ASPX = "aspx"
    private const val EXT_JSP = "jsp"

    private val webpageExtensions =
        setOf(
            EXT_HTML,
            EXT_HTM,
            EXT_PHP,
            EXT_ASP,
            EXT_ASPX,
            EXT_JSP,
        )

    private val contentTypesByExtension =
        mapOf(
            "mp4" to "video/mp4",
            "webm" to "video/webm",
            "mkv" to "video/x-matroska",
            "m3u8" to "application/x-mpegURL",
            "mp3" to "audio/mpeg",
            "flac" to "audio/flac",
        )

    override fun classify(
        url: String,
        contentType: String?,
    ): Verdict {
        val hints = listOfNotNull(url, contentType).joinToString(separator = " ").lowercase(Locale.ROOT)

        return when {
            url.startsWith(BLOB_PREFIX, ignoreCase = true) || hints.contains(MEDIA_SOURCE) ->
                Verdict.Unsupported(UnsupportedReason.MSE_BLOB)
            hints.contains(HVC1) || hints.contains(HEV1) || hints.contains(HEVC) ->
                Verdict.Unsupported(UnsupportedReason.HEVC)
            hints.contains(AV01) || hints.contains(DOT_AV1) ->
                Verdict.Unsupported(UnsupportedReason.AV1)
            hints.contains(WIDEVINE) || hints.contains(DRM_TODAY) || hints.contains(PLAYREADY) ->
                Verdict.Unsupported(UnsupportedReason.DRM)
            isHtmlOrWebpage(url, contentType) ->
                Verdict.Unsupported(UnsupportedReason.NO_MEDIA)
            else -> supportedOrUnknown(url, contentType)
        }
    }

    private fun isHtmlOrWebpage(
        url: String,
        contentType: String?,
    ): Boolean {
        val normalizedType = normalizedContentType(contentType)
        if (normalizedType?.contains(HTML) == true || normalizedType == TEXT_PLAIN) return true
        val ext = extensionOf(url)
        return ext in webpageExtensions
    }

    private fun supportedOrUnknown(
        url: String,
        contentType: String?,
    ): Verdict {
        val knownType = knownTypeForExtension(url) ?: knownTypeForContentType(contentType)
        return knownType?.let(Verdict::Supported) ?: unknownFor(contentType)
    }

    private fun knownTypeForExtension(url: String): String? = contentTypesByExtension[extensionOf(url)]

    private fun knownTypeForContentType(contentType: String?): String? {
        val normalizedType = normalizedContentType(contentType) ?: return null
        return contentTypesByExtension.values.firstOrNull { it.lowercase(Locale.ROOT) == normalizedType }
    }

    private fun unknownFor(contentType: String?): Verdict {
        val normalizedType = normalizedContentType(contentType)
        return when {
            normalizedType == null -> Verdict.Unknown(null)
            normalizedType.startsWith(VIDEO_PREFIX) || normalizedType.startsWith(AUDIO_PREFIX) ->
                Verdict.Unknown(contentType)
            else -> Verdict.Unknown(null)
        }
    }

    private fun normalizedContentType(contentType: String?): String? {
        val normalizedType =
            contentType
                ?.substringBefore(PARAMETER_SEPARATOR)
                ?.trim()
                ?.lowercase(Locale.ROOT)
        return normalizedType?.takeIf { it.isNotEmpty() }
    }

    private fun extensionOf(url: String): String? {
        val path = url.substringBefore(QUERY_SEPARATOR).substringBefore(FRAGMENT_SEPARATOR)
        val lastSegment = path.substringAfterLast(PATH_SEPARATOR)
        if (!lastSegment.contains(EXTENSION_SEPARATOR)) return null
        return lastSegment.substringAfterLast(EXTENSION_SEPARATOR).lowercase(Locale.ROOT)
    }
}
