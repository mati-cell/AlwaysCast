package dev.matiribco.alwayscast.core.network

import android.media.MediaDataSource
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class ProbeWindow(
    val start: Long,
    val bytes: ByteArray,
)

internal class RangeMediaDataSource(
    private val url: String,
    private val client: OkHttpClient,
    private val headers: Map<String, String>,
    private val size: Long,
    private val head: ProbeWindow,
    private val tail: ProbeWindow?,
) : MediaDataSource() {
    override fun readAt(
        position: Long,
        buffer: ByteArray,
        offset: Int,
        readLength: Int,
    ): Int =
        when {
            position < 0 || position >= size -> -1
            else -> readWithinWindows(position, buffer, offset, readLength)
        }

    override fun getSize(): Long = size

    override fun close() = Unit

    private fun readWithinWindows(
        position: Long,
        buffer: ByteArray,
        offset: Int,
        readLength: Int,
    ): Int {
        val available = minOf(size - position, readLength.toLong()).toInt()
        val fromHead = copyFromWindow(head, position, buffer, offset, available)
        val fromTail = tail?.let { copyFromWindow(it, position, buffer, offset, available) } ?: 0

        return when {
            fromHead > 0 -> fromHead
            fromTail > 0 -> fromTail
            else -> fetchRemote(position, available, buffer, offset)
        }
    }

    private fun fetchRemote(
        position: Long,
        length: Int,
        buffer: ByteArray,
        offset: Int,
    ): Int =
        client.newCall(requestFor(position, length)).execute().use { response ->
            val bytes = response.body?.bytes()
            when {
                bytes == null || bytes.isEmpty() -> -1
                else -> {
                    val count = minOf(bytes.size, length)
                    System.arraycopy(bytes, 0, buffer, offset, count)
                    count
                }
            }
        }

    private fun requestFor(
        position: Long,
        length: Int,
    ): Request {
        val builder =
            Request
                .Builder()
                .url(url)
                .header(RANGE_HEADER, "bytes=$position-${position + length - 1}")
        headers.forEach { (name, value) -> builder.header(name, value) }
        return builder.build()
    }

    private fun copyFromWindow(
        window: ProbeWindow,
        position: Long,
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        val delta = position - window.start
        if (delta < 0 || delta >= window.bytes.size) return 0

        val count = minOf(length.toLong(), window.bytes.size - delta).toInt()
        System.arraycopy(window.bytes, delta.toInt(), buffer, offset, count)
        return count
    }

    private companion object {
        const val RANGE_HEADER = "Range"
    }
}
