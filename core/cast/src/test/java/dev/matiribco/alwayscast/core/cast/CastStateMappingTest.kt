package dev.matiribco.alwayscast.core.cast

import com.google.android.gms.cast.CastState
import com.google.android.gms.cast.framework.SessionState
import dev.matiribco.alwayscast.core.model.CastState as DomainCastState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CastStateMappingTest {

    @Test
    fun mapSession_sessionStarted_returnsConnectedWithDeviceName() {
        val state = CastStateMapper.mapSession(SessionState.STARTED, DEVICE_NAME)

        assertEquals(DomainCastState.Connected(DEVICE_NAME), state)
    }

    @Test
    fun mapSession_sessionStarting_returnsConnecting() {
        val state = CastStateMapper.mapSession(SessionState.STARTING, DEVICE_NAME)

        assertEquals(DomainCastState.Connecting, state)
    }

    @Test
    fun mapSession_sessionEnded_returnsIdle() {
        val state = CastStateMapper.mapSession(SessionState.ENDED, DEVICE_NAME)

        assertEquals(DomainCastState.Idle, state)
    }

    @Test
    fun mapCastState_noDevicesAvailable_returnsActionableError() {
        val state = CastStateMapper.mapCastState(CastState.NO_DEVICES_AVAILABLE, null)

        assertTrue(state is DomainCastState.Error && state.message.contains(NO_DEVICE_HINT))
    }

    @Test
    fun mapCastState_connected_returnsConnectedWithDeviceName() {
        val state = CastStateMapper.mapCastState(CastState.CONNECTED, DEVICE_NAME)

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
    }
}
