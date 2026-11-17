package dev.matiribco.alwayscast.core.cast

import dev.matiribco.alwayscast.core.model.CastState as DomainCastState

object CastStateMapper {
    const val LOAD_FAILED_MESSAGE = "Error al cargar el contenido"
    const val NO_DEVICE_HINT = "no se encontro el dispositivo"
    const val DEFAULT_ERROR = "Error desconocido"

    // SDK constants (CastState, SessionState)
    private const val SESSION_STARTED = 2
    private const val SESSION_STARTING = 1
    private const val SESSION_ENDING = 3
    private const val SESSION_ENDED = 4
    private const val SESSION_NO_SESSION = 0

    private const val CAST_NO_DEVICES_AVAILABLE = 1
    private const val CAST_CONNECTED = 3
    private const val CAST_NOT_CONNECTED = 0
    private const val CAST_CONNECTING = 2

    fun mapSession(
        sessionState: Int,
        deviceName: String?,
    ): DomainCastState =
        when (sessionState) {
            SESSION_STARTED -> DomainCastState.Connected(deviceName)
            SESSION_STARTING -> DomainCastState.Connecting
            SESSION_ENDING, SESSION_ENDED, SESSION_NO_SESSION -> DomainCastState.Idle
            else -> DomainCastState.Idle
        }

    fun mapCastState(
        castState: Int,
        deviceName: String?,
    ): DomainCastState =
        when (castState) {
            CAST_NO_DEVICES_AVAILABLE -> DomainCastState.Error(NO_DEVICE_HINT)
            CAST_CONNECTED -> DomainCastState.Connected(deviceName)
            CAST_NOT_CONNECTED, CAST_CONNECTING -> DomainCastState.Connecting
            else -> DomainCastState.Idle
        }

    fun mapLoadError(reason: String?): DomainCastState = DomainCastState.Error(reason ?: LOAD_FAILED_MESSAGE)
}
