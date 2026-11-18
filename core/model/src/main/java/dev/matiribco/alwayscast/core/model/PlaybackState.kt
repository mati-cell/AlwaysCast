package dev.matiribco.alwayscast.core.model

import java.util.Locale

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLive: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String? = null,
) {
    val progressFraction: Float
        get() =
            if (durationMs > 0L) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

    val hasActiveMedia: Boolean
        get() = isPlaying || isBuffering || positionMs > 0L || durationMs > 0L

    val formattedPosition: String
        get() = formatDuration(positionMs)

    val formattedDuration: String
        get() = formatDuration(durationMs)

    companion object {
        val IDLE = PlaybackState()

        private const val SECONDS_PER_MINUTE = 60
        private const val SECONDS_PER_HOUR = 3600
        private const val MILLIS_PER_SECOND_LONG = 1000L

        fun formatDuration(millis: Long): String {
            if (millis <= 0L) return "00:00"
            val totalSeconds = millis / MILLIS_PER_SECOND_LONG
            val seconds = totalSeconds % SECONDS_PER_MINUTE
            val minutes = (totalSeconds / SECONDS_PER_MINUTE) % SECONDS_PER_MINUTE
            val hours = totalSeconds / SECONDS_PER_HOUR
            return if (hours > 0) {
                String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
            }
        }
    }
}
