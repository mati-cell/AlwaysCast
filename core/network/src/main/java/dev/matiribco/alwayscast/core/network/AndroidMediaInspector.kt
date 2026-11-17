package dev.matiribco.alwayscast.core.network

import android.media.MediaExtractor
import android.media.MediaFormat
import dev.matiribco.alwayscast.core.model.MediaCandidate
import dev.matiribco.alwayscast.core.model.UnsupportedReason
import dev.matiribco.alwayscast.core.model.Verdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class AndroidMediaInspector(
    private val client: OkHttpClient,
    private val headersFor: (MediaCandidate) -> Map<String, String> = { candidate -> candidate.headers },
) : MediaInspector {
    override suspend fun inspect(candidate: MediaCandidate): Verdict =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(headRequest(candidate)).execute().use { response ->
                    if (response.isSuccessful) {
                        inspectResponse(
                            candidate,
                            response,
                        )
                    } else {
                        Verdict.Unknown(candidate.contentType)
                    }
                }
            } catch (@Suppress("SwallowedException") exception: Exception) {
                Verdict.Unknown(candidate.contentType)
            }
        }

    private fun inspectResponse(
        candidate: MediaCandidate,
        response: Response,
    ): Verdict {
        val containerContentType = response.header(CONTENT_TYPE) ?: candidate.contentType
        if (containerContentType?.contains("html", ignoreCase = true) == true) {
            return Verdict.Unsupported(UnsupportedReason.NO_MEDIA)
        }
        val size = response.header(CONTENT_LENGTH)?.toLongOrNull() ?: UNKNOWN_SIZE
        val windows = WindowPlanner.rangesFor(size)
        val windowBytes = windows.mapNotNull { range -> fetch(candidate, range) }
        val head = windowBytes.firstOrNull() ?: return Verdict.Unknown(containerContentType)

        return extractVerdict(candidate, ProbePayload(containerContentType, size, windows, windowBytes, head))
    }

    private fun extractVerdict(
        candidate: MediaCandidate,
        payload: ProbePayload,
    ): Verdict {
        val dataSource = payload.dataSourceFor(candidate, client, headersFor(candidate))
        val extractor = MediaExtractor()

        return try {
            extractor.setDataSource(dataSource)
            ProbeVerdicts.fromCodecs(videoMimeOf(extractor), payload.containerContentType)
        } catch (
            @Suppress("SwallowedException") exception: Exception,
        ) {
            // The extractor cannot read these windows; the caller turns that into an Unknown verdict.
            Verdict.Unknown(payload.containerContentType)
        } finally {
            try {
                extractor.release()
            } catch (@Suppress("SwallowedException") ignored: Exception) {
            }
            try {
                dataSource.close()
            } catch (@Suppress("SwallowedException") ignored: Exception) {
            }
        }
    }

    private fun videoMimeOf(extractor: MediaExtractor): String? =
        (0 until extractor.trackCount)
            .mapNotNull { index -> extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME) }
            .firstOrNull { mime -> mime.startsWith(VIDEO_PREFIX) }

    private fun fetch(
        candidate: MediaCandidate,
        range: LongRange,
    ): ByteArray? {
        val builder =
            Request
                .Builder()
                .url(candidate.url)
                .header(RANGE, "bytes=${range.first}-${range.last}")
        headersFor(candidate).forEach { (name, value) -> builder.header(name, value) }

        return client.newCall(builder.build()).execute().use { response ->
            if (response.isSuccessful) response.body?.bytes() else null
        }
    }

    private fun headRequest(candidate: MediaCandidate): Request {
        val builder = Request.Builder().url(candidate.url).head()
        headersFor(candidate).forEach { (name, value) -> builder.header(name, value) }
        return builder.build()
    }

    private companion object {
        const val UNKNOWN_SIZE = -1L
        const val VIDEO_PREFIX = "video/"
        const val CONTENT_TYPE = "Content-Type"
        const val CONTENT_LENGTH = "Content-Length"
        const val RANGE = "Range"
    }
}

private data class ProbePayload(
    val containerContentType: String?,
    val size: Long,
    val windows: List<LongRange>,
    val windowBytes: List<ByteArray>,
    val head: ByteArray,
) {
    fun dataSourceFor(
        candidate: MediaCandidate,
        client: OkHttpClient,
        headers: Map<String, String>,
    ): RangeMediaDataSource {
        val headWindow = ProbeWindow(windows.first().first, head)
        val tailWindow =
            if (windows.size > 1 && windowBytes.size > 1) {
                ProbeWindow(windows[1].first, windowBytes[1])
            } else {
                null
            }

        return RangeMediaDataSource(
            url = candidate.url,
            client = client,
            headers = headers,
            size = if (size >= 0) size else head.size.toLong(),
            head = headWindow,
            tail = tailWindow,
        )
    }
}
