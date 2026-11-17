package dev.matiribco.alwayscast.core.model

sealed interface CastState {
    data object Idle : CastState

    data object Connecting : CastState

    data class Connected(
        val deviceName: String?,
    ) : CastState

    data class Error(
        val message: String,
    ) : CastState
}
