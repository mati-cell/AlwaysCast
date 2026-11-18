package dev.matiribco.alwayscast.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun idleState_hasNoActiveMediaAndZeroProgress() {
        val state = PlaybackState.IDLE

        assertFalse(state.hasActiveMedia)
        assertEquals(0f, state.progressFraction, 0.001f)
    }

    @Test
    fun playingState_computesCorrectProgressFraction() {
        val state =
            PlaybackState(
                isPlaying = true,
                positionMs = 30_000L,
                durationMs = 60_000L,
                title = "Video Title",
            )

        assertTrue(state.hasActiveMedia)
        assertEquals(0.5f, state.progressFraction, 0.001f)
    }

    @Test
    fun liveStream_zeroDuration_returnsZeroProgressFraction() {
        val state =
            PlaybackState(
                isPlaying = true,
                isLive = true,
                positionMs = 120_000L,
                durationMs = 0L,
            )

        assertTrue(state.hasActiveMedia)
        assertEquals(0f, state.progressFraction, 0.001f)
    }
}
