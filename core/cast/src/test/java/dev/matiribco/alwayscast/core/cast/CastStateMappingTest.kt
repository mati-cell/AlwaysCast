package dev.matiribco.alwayscast.core.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import dev.matiribco.alwayscast.core.model.CastState as DomainCastState

class CastStateMappingTest {
    @Test
    fun mapSession_sessionStarted_returnsConnectedWithDeviceName() {
        val state = CastStateMapper.mapSession(SESSION_STARTED, DEVICE_NAME)

        assertEquals(DomainCastState.Connected(DEVICE_NAME), state)
    }

    @Test
    fun mapSession_sessionStarting_returnsConnecting() {
        val state = CastStateMapper.mapSession(SESSION_STARTING, DEVICE_NAME)

        assertEquals(DomainCastState.Connecting, state)
    }

    @Test
    fun mapSession_sessionEnded_returnsIdle() {
        val state = CastStateMapper.mapSession(SESSION_ENDED, DEVICE_NAME)

        assertEquals(DomainCastState.Idle, state)
    }

    @Test
    fun mapCastState_noDevicesAvailable_returnsActionableError() {
        val state = CastStateMapper.mapCastState(CAST_NO_DEVICES_AVAILABLE, null)

        assertTrue(state is DomainCastState.Error && state.message.contains("encontro"))
    }

    @Test
    fun mapCastState_connected_returnsConnectedWithDeviceName() {
        val state = CastStateMapper.mapCastState(CAST_CONNECTED, DEVICE_NAME)

        assertEquals(DomainCastState.Connected(DEVICE_NAME), state)
    }

    @Test
    fun mapLoadError_withoutReason_returnsDefaultError() {
        val state = CastStateMapper.mapLoadError(null)

        assertEquals(DomainCastState.Error(CastStateMapper.LOAD_FAILED_MESSAGE), state)
    }

    @Test
    fun mapLoadError_withReason_returnsErrorWithThatReason() {
        val state = CastStateMapper.mapLoadError(REASON)

        assertEquals(DomainCastState.Error(REASON), state)
    }

    private companion object {
        const val DEVICE_NAME = "Sala familiar"
        const val REASON = "VIDEO_VIEW_NOT_FOUND"
        const val NO_DEVICE_HINT = "no encontr"

        // SDK constants (CastState, SessionState)
        const val SESSION_STARTED = 2
        const val SESSION_STARTING = 1
        const val SESSION_ENDING = 3
        const val SESSION_ENDED = 4
        const val SESSION_NO_SESSION = 0
        const val CAST_NO_DEVICES_AVAILABLE = 1
        const val CAST_CONNECTED = 3
        const val CAST_NOT_CONNECTED = 0
        const val CAST_CONNECTING = 2
    }
}
