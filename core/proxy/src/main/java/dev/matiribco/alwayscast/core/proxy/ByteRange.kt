package dev.matiribco.alwayscast.core.proxy

data class ByteRange(
    val start: Long,
    val endInclusive: Long,
) {
    val length: Long
        get() = endInclusive - start + 1

    fun contentRangeHeader(size: Long): String = "bytes $start-$endInclusive/$size"

    companion object {
        private const val PREFIX = "bytes="
        private const val RANGE_SEPARATOR = '-'

        fun parse(
            header: String,
            size: Long,
        ): ByteRange? =
            header
                .takeIf { it.startsWith(PREFIX) }
                ?.removePrefix(PREFIX)
                ?.substringBefore(',')
                ?.let { parseSpec(it.trim(), size) }
                ?.let { ByteRange(it.start, it.endInclusive.coerceAtMost(size - 1)) }
                ?.takeIf { it.start in 0 until size && it.endInclusive >= it.start }

        private fun parseSpec(
            spec: String,
            size: Long,
        ): ByteRange? {
            val separator = spec.indexOf(RANGE_SEPARATOR)
            if (separator < 0) return null

            val startText = spec.substring(0, separator).trim()
            val endText = spec.substring(separator + 1).trim()

            return when {
                startText.isEmpty() -> suffixRange(endText, size)
                endText.isEmpty() -> startText.toLongOrNull()?.let { ByteRange(it, size - 1) }
                else -> closedRange(startText, endText)
            }
        }

        private fun suffixRange(
            endText: String,
            size: Long,
        ): ByteRange? =
            endText
                .toLongOrNull()
                ?.takeIf { it > 0 && size > 0 }
                ?.let { suffixLength -> ByteRange((size - suffixLength).coerceAtLeast(0), size - 1) }

        private fun closedRange(
            startText: String,
            endText: String,
        ): ByteRange? {
            val start = startText.toLongOrNull()
            val end = endText.toLongOrNull()
            return if (start != null && end != null) ByteRange(start, end) else null
        }
    }
}
