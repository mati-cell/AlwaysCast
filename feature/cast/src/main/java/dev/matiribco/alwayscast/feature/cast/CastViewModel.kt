package dev.matiribco.alwayscast.feature.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matiribco.alwayscast.core.cast.CastController
import dev.matiribco.alwayscast.core.model.MediaCandidate
import dev.matiribco.alwayscast.core.model.MediaSourceResolver
import dev.matiribco.alwayscast.core.model.UnsupportedReason
import dev.matiribco.alwayscast.core.model.Verdict
import dev.matiribco.alwayscast.core.network.MediaClassifier
import dev.matiribco.alwayscast.core.network.MediaInspector
import dev.matiribco.alwayscast.core.network.WebMediaExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class CastViewModel(
    private val classifier: MediaClassifier,
    private val resolver: MediaSourceResolver,
    private val controller: CastController,
    private val inspector: MediaInspector? = null,
    private val extractor: WebMediaExtractor? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(CastUiState.idle())
    val state: StateFlow<CastUiState> = _state.asStateFlow()

    private var currentUrl: String = ""

    init {
        viewModelScope.launch {
            controller.playbackState.collect { playback ->
                _state.value = _state.value.copy(playback = playback)
            }
        }
    }

    fun onPlayOrPause() {
        controller.playOrPause()
    }

    fun onSeekTo(positionMs: Long) {
        controller.seekToMillis(positionMs)
    }

    fun onForward() {
        controller.forward(DEFAULT_SEEK_SECONDS)
    }

    fun onRewind() {
        controller.rewind(DEFAULT_SEEK_SECONDS)
    }

    fun onStop() {
        controller.stop()
    }

    fun onUrlChange(url: String) {
        currentUrl = url.trim()
    }

    fun onCastClick() {
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _state.value = CastUiState.probing(currentUrl)

            val candidate = MediaCandidate(url = currentUrl)
            var verdict = classifier.classify(candidate.url, candidate.contentType)

            if (verdict is Verdict.Unknown && inspector != null) {
                verdict = inspector.inspect(candidate)
            }

            if (shouldAttemptExtraction(verdict) && extractor != null) {
                val extracted = extractor.extract(currentUrl)
                if (extracted.size > 1) {
                    _state.value =
                        CastUiState.selectingCandidate(
                            url = currentUrl,
                            candidates = extracted,
                            deviceName = _state.value.deviceName,
                        )
                    return@launch
                } else if (extracted.size == 1) {
                    castCandidate(extracted.first())
                    return@launch
                }
            }

            handleVerdict(verdict, candidate)
        }
    }

    fun onSelectCandidate(candidate: MediaCandidate) {
        viewModelScope.launch {
            castCandidate(candidate)
        }
    }

    fun onConnectClick() {
        viewModelScope.launch {
            val connectResult = controller.connect()
            if (connectResult.isFailure) {
                _state.value =
                    _state.value.copy(
                        message = connectResult.exceptionOrNull()?.message ?: NO_DEVICE_FOUND_MESSAGE,
                    )
                return@launch
            }

            val deviceName = connectResult.getOrNull() ?: DEFAULT_DEVICE_NAME
            if (_state.value.phase == Phase.SELECTING_CANDIDATE) {
                _state.value = _state.value.copy(deviceName = deviceName)
            } else if (currentUrl.isNotBlank()) {
                val candidate = MediaCandidate(url = currentUrl)
                castCandidate(candidate, deviceName)
            } else {
                _state.value = CastUiState.ready(currentUrl, deviceName)
            }
        }
    }

    private suspend fun handleVerdict(
        verdict: Verdict,
        candidate: MediaCandidate,
    ) {
        when (verdict) {
            is Verdict.Unsupported -> {
                val message = formatUnsupportedMessage(verdict.reason)
                _state.value = CastUiState.unsupported(currentUrl, message)
            }
            is Verdict.Unknown -> {
                _state.value = CastUiState.unsupported(currentUrl, UNKNOWN_MEDIA_MESSAGE)
            }
            is Verdict.Supported -> {
                castCandidate(candidate)
            }
        }
    }

    private suspend fun castCandidate(
        candidate: MediaCandidate,
        connectedDeviceName: String? = null,
    ) {
        val deviceName =
            connectedDeviceName ?: run {
                val connectResult = controller.connect()
                if (connectResult.isFailure) {
                    val message = connectResult.exceptionOrNull()?.message ?: NO_DEVICE_FOUND_MESSAGE
                    _state.value = CastUiState.error(message)
                    return
                }
                connectResult.getOrNull() ?: DEFAULT_DEVICE_NAME
            }

        _state.value = CastUiState.ready(candidate.url, deviceName)
        val resolved = resolver.resolve(candidate)
        val loadResult = controller.load(resolved, candidate.label)

        if (loadResult.isFailure) {
            val reason = loadResult.exceptionOrNull()?.message ?: LOAD_ERROR_DEFAULT
            _state.value = CastUiState.error("$LOAD_ERROR_PREFIX: $reason")
            return
        }

        _state.value = CastUiState.casting(resolved.url, deviceName)
    }

    private fun shouldAttemptExtraction(verdict: Verdict): Boolean =
        when (verdict) {
            is Verdict.Unsupported -> verdict.reason == UnsupportedReason.NO_MEDIA
            is Verdict.Unknown -> true
            is Verdict.Supported -> false
        }

    private fun formatUnsupportedMessage(reason: UnsupportedReason): String =
        when (reason) {
            UnsupportedReason.HEVC -> HEVC_UNSUPPORTED_MESSAGE
            UnsupportedReason.AV1 -> AV1_UNSUPPORTED_MESSAGE
            UnsupportedReason.DRM -> DRM_UNSUPPORTED_MESSAGE
            UnsupportedReason.MSE_BLOB -> MSE_BLOB_UNSUPPORTED_MESSAGE
            UnsupportedReason.NO_MEDIA -> NO_MEDIA_UNSUPPORTED_MESSAGE
        }

    private companion object {
        const val DEFAULT_SEEK_SECONDS = 10.0
        const val DEFAULT_DEVICE_NAME = "Chromecast"
        const val NO_DEVICE_FOUND_MESSAGE = "No se encontró el Chromecast en la red"
        const val LOAD_ERROR_PREFIX = "Error al cargar el video"
        const val LOAD_ERROR_DEFAULT = "Fallo desconocido"
        const val HEVC_UNSUPPORTED_MESSAGE = "Códec HEVC no soportado por Chromecast de 1ª generación"
        const val AV1_UNSUPPORTED_MESSAGE = "Códec AV1 no soportado por Chromecast de 1ª generación"
        const val DRM_UNSUPPORTED_MESSAGE = "Contenido con DRM no soportado"
        const val MSE_BLOB_UNSUPPORTED_MESSAGE = "Streams blob/mediasource no soportados"
        const val NO_MEDIA_UNSUPPORTED_MESSAGE =
            "No se detectó un video directo (la URL es una página web HTML o un enlace no compatible)"
        const val UNKNOWN_MEDIA_MESSAGE =
            "No se detectó un archivo de video compatible en este enlace"
    }
}
