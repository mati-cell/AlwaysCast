package dev.matiribco.alwayscast.core.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
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

    private var connectDeferred: CompletableDeferred<Result<String>> = CompletableDeferred<Result<String>>()
    private var loadDeferred: CompletableDeferred<Result<Unit>> = CompletableDeferred<Result<Unit>>()
    private var currentSession: CastSession? = null

    init {
        updateStateFromSession(sessionManager.currentCastSession)
    }

    override suspend fun connect(): Result<String> {
        connectDeferred = CompletableDeferred<Result<String>>()

        val session = sessionManager.currentCastSession
        if (session != null) {
            return Result.success("Chromecast")
        }

        // sessionManager.startSession() requires parameters in this SDK version

        return connectDeferred.await()
    }

    override suspend fun load(
        source: ResolvedSource,
        title: String?,
    ): Result<Unit> {
        loadDeferred = CompletableDeferred<Result<Unit>>()

        val session = currentSession ?: return Result.failure(IllegalStateException("No active session"))

        // Implementation deferred to Task 8 (device testing)
        loadDeferred.complete(Result.success(Unit))

        return loadDeferred.await()
    }

    override fun playOrPause() {
        // Implementation deferred to Task 8 (device testing)
    }

    override fun seekTo(seconds: Double) {
        // Implementation deferred to Task 8 (device testing)
    }

    override fun stop() {
        // Implementation deferred to Task 8 (device testing)
    }

    override fun disconnect() {
        sessionManager.endCurrentSession(true)
    }

    private fun updateStateFromSession(session: CastSession?) {
        val domainState =
            when {
                session == null -> DomainCastState.Idle
                else -> DomainCastState.Connected("Chromecast")
            }
        stateFlow.value = domainState
    }
}
