package dev.matiribco.alwayscast.core.cast

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import dev.matiribco.alwayscast.core.model.ResolvedSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.Result
import dev.matiribco.alwayscast.core.model.CastState as DomainCastState

class GoogleCastController(
    private val context: Context,
    private val castContext: CastContext = CastContext.getSharedInstance(context),
) : CastController {
    private val sessionManager = castContext.sessionManager
    private val stateFlow = MutableStateFlow<DomainCastState>(DomainCastState.Idle)
    override val state = stateFlow.asStateFlow()

    private var connectDeferred: CompletableDeferred<Result<String>>? = null
    private var currentSession: CastSession? = null

    private val sessionManagerListener =
        object : SessionManagerListener<CastSession> {
            override fun onSessionStarting(session: CastSession) {
                stateFlow.value = DomainCastState.Connecting
            }

            override fun onSessionStarted(
                session: CastSession,
                sessionId: String,
            ) {
                currentSession = session
                val name = session.castDevice?.friendlyName ?: DEFAULT_DEVICE_NAME
                stateFlow.value = DomainCastState.Connected(name)
                connectDeferred?.complete(Result.success(name))
            }

            override fun onSessionStartFailed(
                session: CastSession,
                error: Int,
            ) {
                currentSession = null
                val errorMsg = "Error al iniciar sesión de Cast ($error)"
                stateFlow.value = DomainCastState.Error(errorMsg)
                connectDeferred?.complete(Result.failure(IllegalStateException(errorMsg)))
            }

            override fun onSessionEnding(session: CastSession) {
                stateFlow.value = DomainCastState.Idle
            }

            override fun onSessionEnded(
                session: CastSession,
                error: Int,
            ) {
                currentSession = null
                stateFlow.value = DomainCastState.Idle
            }

            override fun onSessionResuming(
                session: CastSession,
                sessionId: String,
            ) {
                stateFlow.value = DomainCastState.Connecting
            }

            override fun onSessionResumed(
                session: CastSession,
                wasSuspended: Boolean,
            ) {
                currentSession = session
                val name = session.castDevice?.friendlyName ?: DEFAULT_DEVICE_NAME
                stateFlow.value = DomainCastState.Connected(name)
            }

            override fun onSessionResumeFailed(
                session: CastSession,
                error: Int,
            ) {
                currentSession = null
                stateFlow.value = DomainCastState.Idle
            }

            override fun onSessionSuspended(
                session: CastSession,
                reason: Int,
            ) {
                stateFlow.value = DomainCastState.Idle
            }
        }

    init {
        sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
        updateStateFromSession(sessionManager.currentCastSession)
    }

    override suspend fun connect(): Result<String> {
        val session = sessionManager.currentCastSession
        if (session != null && session.isConnected) {
            val name = session.castDevice?.friendlyName ?: DEFAULT_DEVICE_NAME
            currentSession = session
            return Result.success(name)
        }

        return Result.failure(IllegalStateException(CONNECT_PROMPT_MESSAGE))
    }

    override suspend fun load(
        source: ResolvedSource,
        title: String?,
    ): Result<Unit> {
        val session = currentSession ?: sessionManager.currentCastSession
        val remoteMediaClient = session?.remoteMediaClient

        if (session == null || remoteMediaClient == null) {
            val message = if (session == null) NO_ACTIVE_SESSION_MESSAGE else NO_MEDIA_CLIENT_MESSAGE
            return Result.failure(IllegalStateException(message))
        }

        return runCatching {
            val metadata =
                MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                    putString(MediaMetadata.KEY_TITLE, title ?: DEFAULT_TITLE)
                }

            val mediaInfo =
                MediaInfo
                    .Builder(source.url)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setContentType(source.contentType)
                    .setMetadata(metadata)
                    .build()

            val request =
                MediaLoadRequestData
                    .Builder()
                    .setMediaInfo(mediaInfo)
                    .setAutoplay(true)
                    .build()

            remoteMediaClient.load(request)
            Unit
        }
    }

    override fun playOrPause() {
        val rmc = (currentSession ?: sessionManager.currentCastSession)?.remoteMediaClient ?: return
        if (rmc.isPlaying) {
            rmc.pause()
        } else {
            rmc.play()
        }
    }

    override fun seekTo(seconds: Double) {
        val rmc = (currentSession ?: sessionManager.currentCastSession)?.remoteMediaClient ?: return
        rmc.seek((seconds * MILLIS_PER_SECOND).toLong())
    }

    override fun stop() {
        (currentSession ?: sessionManager.currentCastSession)?.remoteMediaClient?.stop()
    }

    override fun disconnect() {
        sessionManager.endCurrentSession(true)
        currentSession = null
        stateFlow.value = DomainCastState.Idle
    }

    private fun updateStateFromSession(session: CastSession?) {
        currentSession = session
        val domainState =
            when {
                session == null -> DomainCastState.Idle
                session.isConnected ->
                    DomainCastState.Connected(
                        session.castDevice?.friendlyName ?: DEFAULT_DEVICE_NAME,
                    )
                else -> DomainCastState.Connecting
            }
        stateFlow.value = domainState
    }

    private companion object {
        const val DEFAULT_DEVICE_NAME = "Chromecast"
        const val DEFAULT_TITLE = "AlwaysCast Stream"
        const val MILLIS_PER_SECOND = 1000.0
        const val NO_ACTIVE_SESSION_MESSAGE =
            "No hay un Chromecast conectado. Selecciona tu dispositivo con el icono de Cast."
        const val NO_MEDIA_CLIENT_MESSAGE =
            "El cliente de reproduccion del Chromecast no esta disponible"
        const val CONNECT_PROMPT_MESSAGE =
            "Toca el icono de Cast en la barra superior para seleccionar tu Chromecast"
    }
}
