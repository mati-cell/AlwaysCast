package dev.matiribco.alwayscast.feature.cast

enum class Phase {
    IDLE,
    PROBING,
    READY,
    CASTING,
    UNSUPPORTED,
    ERROR,
}

data class CastUiState(
    val url: String = "",
    val phase: Phase = Phase.IDLE,
    val deviceName: String? = null,
    val message: String? = null,
) {
    companion object {
        fun idle(): CastUiState = CastUiState()

        fun probing(url: String): CastUiState = CastUiState(url = url, phase = Phase.PROBING)

        fun ready(
            url: String,
            deviceName: String?,
        ): CastUiState = CastUiState(url = url, phase = Phase.READY, deviceName = deviceName)

        fun casting(
            url: String,
            deviceName: String?,
        ): CastUiState = CastUiState(url = url, phase = Phase.CASTING, deviceName = deviceName)

        fun unsupported(
            url: String,
            reason: String,
        ): CastUiState = CastUiState(url = url, phase = Phase.UNSUPPORTED, message = reason)

        fun error(message: String): CastUiState = CastUiState(phase = Phase.ERROR, message = message)
    }
}
