package dev.matiribco.alwayscast.feature.cast

enum class Phase {
    IDLE,
    PROBING,
    SELECTING_CANDIDATE,
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
    val candidates: List<MediaCandidate> = emptyList(),
) {
    companion object {
        fun idle(): CastUiState = CastUiState()

        fun probing(url: String): CastUiState = CastUiState(url = url, phase = Phase.PROBING)

        fun selectingCandidate(
            url: String,
            candidates: List<MediaCandidate>,
            deviceName: String? = null,
        ): CastUiState =
            CastUiState(
                url = url,
                phase = Phase.SELECTING_CANDIDATE,
                candidates = candidates,
                deviceName = deviceName,
            )

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
