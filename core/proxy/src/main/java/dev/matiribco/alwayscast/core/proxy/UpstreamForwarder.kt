package dev.matiribco.alwayscast.core.proxy

import dev.matiribco.alwayscast.core.model.MediaCandidate
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

internal class UpstreamForwarder(
    private val client: OkHttpClient,
    private val spool: SpoolStore,
    private val mapUri: (String) -> String,
) {
    private val candidates = ConcurrentHashMap<String, MediaCandidate>()
    private val spooledContentTypes = ConcurrentHashMap<String, String>()

    fun register(candidate: MediaCandidate) {
        candidates[candidate.url] = candidate
    }

    suspend fun forward(
        call: ApplicationCall,
        upstream: String,
    ) {
        val range = call.request.headers[HttpHeaders.Range]
        val spooledFile = spooledFileFor(upstream)

        if (range != null && spooledFile != null) {
            call.serveSpooledFile(upstream, spooledFile, range)
        } else {
            forwardToOrigin(call, upstream, range)
        }
    }

    private suspend fun forwardToOrigin(
        call: ApplicationCall,
        upstream: String,
        range: String?,
    ) {
        val originResponse = withContext(Dispatchers.IO) { client.newCall(requestFor(upstream, range)).execute() }
        originResponse.use {
            val contentType = it.header(HttpHeaders.ContentType) ?: candidates[upstream]?.contentType
            when {
                range != null && it.code == HTTP_OK -> call.spoolAndServe(upstream, it, range, contentType)
                isPlaylist(upstream, contentType) -> call.respondPlaylist(upstream, it, contentType)
                else -> call.respondStreaming(it, contentType)
            }
        }
    }

    private fun spooledFileFor(upstream: String): File? {
        if (!spooledContentTypes.containsKey(upstream)) return null
        return spool.file(spoolKey(upstream)).takeIf { it.length() > 0 }
    }

    private suspend fun ApplicationCall.spoolAndServe(
        upstream: String,
        originResponse: Response,
        range: String,
        contentType: String?,
    ) {
        val file = spool.file(spoolKey(upstream))
        withContext(Dispatchers.IO) {
            originResponse.body?.byteStream()?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        spooledContentTypes[upstream] = contentType ?: ContentType.Application.OctetStream.toString()
        serveSpooledFile(upstream, file, range)
    }

    private suspend fun ApplicationCall.serveSpooledFile(
        upstream: String,
        file: File,
        range: String,
    ) {
        val byteRange = ByteRange.parse(range, file.length())
        if (byteRange == null) {
            respond(HttpStatusCode.RequestedRangeNotSatisfiable)
            return
        }

        response.header(HttpHeaders.ContentRange, byteRange.contentRangeHeader(file.length()))
        response.header(HttpHeaders.AcceptRanges, BYTES)
        respondOutputStream(
            contentType = ContentType.parse(spooledContentTypes.getValue(upstream)),
            status = HttpStatusCode.PartialContent,
            contentLength = byteRange.length,
        ) {
            writeRange(file, byteRange)
        }
    }

    private suspend fun ApplicationCall.respondPlaylist(
        upstream: String,
        originResponse: Response,
        contentType: String?,
    ) {
        val body = originResponse.body
        if (body == null || body.contentLength() !in 0..PLAYLIST_MAX_BYTES) {
            respondStreaming(originResponse, contentType)
            return
        }

        val rewritten = HlsPlaylistRewriter(mapUri).rewrite(body.string(), upstream)
        respondText(
            text = rewritten.text,
            contentType = ContentType.parse(contentType ?: HLS_CONTENT_TYPE),
            status = HttpStatusCode.fromValue(originResponse.code),
        )
    }

    private suspend fun ApplicationCall.respondStreaming(
        originResponse: Response,
        contentType: String?,
    ) {
        originResponse.header(HttpHeaders.ContentRange)?.let { response.header(HttpHeaders.ContentRange, it) }
        originResponse.header(HttpHeaders.AcceptRanges)?.let { response.header(HttpHeaders.AcceptRanges, it) }

        val body = originResponse.body
        if (body == null) {
            respond(HttpStatusCode.fromValue(originResponse.code))
            return
        }

        respondOutputStream(
            contentType = contentType?.let(ContentType::parse),
            status = HttpStatusCode.fromValue(originResponse.code),
            contentLength = body.contentLength().takeIf { it >= 0 },
        ) {
            body.byteStream().use { input -> input.copyTo(this) }
        }
    }

    private fun requestFor(
        upstream: String,
        range: String?,
    ): Request {
        val candidate = candidates[upstream] ?: MediaCandidate(url = upstream)
        val builder = Request.Builder().url(upstream).get()

        HeaderInjector.headersFor(candidate).forEach { (name, value) -> builder.header(name, value) }
        range?.let { builder.header(HttpHeaders.Range, it) }

        return builder.build()
    }

    private fun isPlaylist(
        upstream: String,
        contentType: String?,
    ): Boolean =
        contentType?.contains(MPEGURL_HINT, ignoreCase = true) == true ||
            upstream.substringBefore('?').endsWith(HLS_EXTENSION, ignoreCase = true)
}

private const val HTTP_OK = 200
private const val BYTES = "bytes"
private const val PLAYLIST_MAX_BYTES = 2L * 1024L * 1024L
private const val MPEGURL_HINT = "mpegurl"
private const val HLS_EXTENSION = ".m3u8"
private const val HLS_CONTENT_TYPE = "application/vnd.apple.mpegurl"
private const val SHA_256 = "SHA-256"

private fun spoolKey(upstream: String): String {
    val digest = MessageDigest.getInstance(SHA_256).digest(upstream.toByteArray(StandardCharsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun OutputStream.writeRange(
    file: File,
    range: ByteRange,
) {
    RandomAccessFile(file, "r").use { source ->
        source.seek(range.start)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var remaining = range.length
        while (remaining > 0) {
            val read = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) break
            write(buffer, 0, read)
            remaining -= read
        }
    }
}
