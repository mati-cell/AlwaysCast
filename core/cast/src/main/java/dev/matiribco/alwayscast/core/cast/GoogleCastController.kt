package dev.matiribco.alwayscast.core.cast

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import dev.matiribco.alwayscast.core.model.PlaybackState
import dev.matiribco.alwayscast.core.model.ResolvedSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.Result
import dev.matiribco.alwayscast.core.model.CastState as DomainCastState

@Suppress("TooManyFunctions", "DEPRECATION")
class GoogleCastController(
    private val context: Context,
    private val castContext: CastContext = CastContext.getSharedInstance(context),
) : CastController {
    private val sessionManager = castContext.sessionManager
    private val stateFlow = MutableStateFlow<DomainCastState>(DomainCastState.Idle)
    override val state = stateFlow.asStateFlow()

    private val playbackStateFlow = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState = playbackStateFlow.asStateFlow()

    private var connectDeferred: CompletableDeferred<Result<String>>? = null
    private var currentSession: CastSession? = null

    private val mediaCallback =
        object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                syncPlaybackState()
            }

            override fun onMetadataUpdated() {
                syncPlaybackState()
            }
        }

    private val progressListener =
        RemoteMediaClient.ProgressListener { progressMs, durationMs ->
            syncPlaybackState(progressOverride = progressMs, durationOverride = durationMs)
        }

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
                attachRemoteMediaClient(session)
                val name = session.castDevice?.friendlyName ?: DEFAULT_DEVICE_NAME
                stateFlow.value = DomainCastState.Connected(name)
                connectDeferred?.complete(Result.success(name))
            }

            override fun onSessionStartFailed(
                session: CastSession,
                error: Int,
            ) {
                detachRemoteMediaClient(currentSession)
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
                detachRemoteMediaClient(session)
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
                attachRemoteMediaClient(session)
                val name = session.castDevice?.friendlyName ?: DEFAULT_DEVICE_NAME
                stateFlow.value = DomainCastState.Connected(name)
            }

            override fun onSessionResumeFailed(
                session: CastSession,
                error: Int,
            ) {
                detachRemoteMediaClient(currentSession)
                currentSession = null
                stateFlow.value = DomainCastState.Idle
            }

            override fun onSessionSuspended(
                session: CastSession,
                reason: Int,
            ) {
                detachRemoteMediaClient(session)
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
            attachRemoteMediaClient(session)
            Unit
        }
    }

    override fun play() {
        (currentSession ?: sessionManager.currentCastSession)?.remoteMediaClient?.play()
    }

    override fun pause() {
        (currentSession ?: sessionManager.currentCastSession)?.remoteMediaClient?.pause()
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
        seekToMillis((seconds * MILLIS_PER_SECOND).toLong())
    }

    override fun seekToMillis(positionMs: Long) {
        val rmc = (currentSession ?: sessionManager.currentCastSession)?.remoteMediaClient ?: return
        rmc.seek(positionMs.coerceAtLeast(0L))
    }

    override fun forward(seconds: Double) {
        val rmc = (currentSession ?: sessionManager.currentCastSession)?.remoteMediaClient ?: return
        val currentPos = rmc.approximateStreamPosition
        val duration = rmc.streamDuration
        val deltaMs = (seconds * MILLIS_PER_SECOND).toLong()
        val targetPos =
            if (duration > 0) {
                (currentPos + deltaMs).coerceAtMost(duration)
            } else {
                currentPos + deltaMs
            }
        rmc.seek(targetPos)
    }

    override fun rewind(seconds: Double) {
        val rmc = (currentSession ?: sessionManager.currentCastSession)?.remoteMediaClient ?: return
        val currentPos = rmc.approximateStreamPosition
        val deltaMs = (seconds * MILLIS_PER_SECOND).toLong()
        val targetPos = (currentPos - deltaMs).coerceAtLeast(0L)
        rmc.seek(targetPos)
    }

    override fun stop() {
        (currentSession ?: sessionManager.currentCastSession)?.remoteMediaClient?.stop()
        playbackStateFlow.value = PlaybackState.IDLE
    }

    override fun disconnect() {
        detachRemoteMediaClient(currentSession)
        sessionManager.endCurrentSession(true)
        currentSession = null
        stateFlow.value = DomainCastState.Idle
    }

    private fun attachRemoteMediaClient(session: CastSession?) {
        val rmc = session?.remoteMediaClient ?: return
        rmc.registerCallback(mediaCallback)
        rmc.addProgressListener(progressListener, PROGRESS_INTERVAL_MS)
        syncPlaybackState()
    }

    private fun detachRemoteMediaClient(session: CastSession?) {
        val rmc = session?.remoteMediaClient
        if (rmc != null) {
            rmc.unregisterCallback(mediaCallback)
            rmc.removeProgressListener(progressListener)
        }
        playbackStateFlow.value = PlaybackState.IDLE
    }

    private fun syncPlaybackState(
        progressOverride: Long? = null,
        durationOverride: Long? = null,
    ) {
        val rmc = (currentSession ?: sessionManager.currentCastSession)?.remoteMediaClient
        if (rmc == null || rmc.mediaStatus == null) {
            playbackStateFlow.value = PlaybackState.IDLE
            return
        }

        val playerState = rmc.playerState
        val isPlaying = playerState == MediaStatus.PLAYER_STATE_PLAYING
        val isBuffering = playerState == MediaStatus.PLAYER_STATE_BUFFERING
        val isLive = rmc.mediaInfo?.streamType == MediaInfo.STREAM_TYPE_LIVE
        val positionMs = progressOverride ?: rmc.approximateStreamPosition.coerceAtLeast(0L)
        val rawDuration = durationOverride ?: rmc.streamDuration
        val durationMs = if (rawDuration > 0L) rawDuration else 0L
        val title = rmc.mediaInfo?.metadata?.getString(MediaMetadata.KEY_TITLE)

        playbackStateFlow.value =
            PlaybackState(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                isLive = isLive,
                positionMs = positionMs,
                durationMs = durationMs,
                title = title,
            )
    }

    private fun updateStateFromSession(session: CastSession?) {
        currentSession = session
        if (session != null && session.isConnected) {
            attachRemoteMediaClient(session)
        } else {
            detachRemoteMediaClient(session)
        }
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
        const val PROGRESS_INTERVAL_MS = 1000L
        const val NO_ACTIVE_SESSION_MESSAGE =
            "No hay un Chromecast conectado. Selecciona tu dispositivo con el icono de Cast."
        const val NO_MEDIA_CLIENT_MESSAGE =
            "El cliente de reproduccion del Chromecast no esta disponible"
        const val CONNECT_PROMPT_MESSAGE =
            "Toca el icono de Cast en la barra superior para seleccionar tu Chromecast"
    }
}
