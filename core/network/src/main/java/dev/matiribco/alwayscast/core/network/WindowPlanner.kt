package dev.matiribco.alwayscast.core.network

object WindowPlanner {
    const val HEAD_BYTES = 4L * 1024L * 1024L
    const val TAIL_BYTES = 2L * 1024L * 1024L

    fun rangesFor(
        size: Long,
        head: Long = HEAD_BYTES,
        tail: Long = TAIL_BYTES,
    ): List<LongRange> =
        when {
            size < 0 -> listOf(0L..head - 1)
            size == 0L -> emptyList()
            size <= head + tail -> listOf(0L..size - 1)
            else -> listOf(0L..head - 1, (size - tail)..(size - 1))
        }
}
