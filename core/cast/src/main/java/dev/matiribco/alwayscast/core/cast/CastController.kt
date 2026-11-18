package dev.matiribco.alwayscast.core.cast

import dev.matiribco.alwayscast.core.model.CastState
import dev.matiribco.alwayscast.core.model.PlaybackState
import dev.matiribco.alwayscast.core.model.ResolvedSource
import kotlinx.coroutines.flow.Flow
import kotlin.Result

@Suppress("TooManyFunctions")
interface CastController {
    val state: Flow<CastState>
    val playbackState: Flow<PlaybackState>

    suspend fun connect(): Result<String>

    suspend fun load(
        source: ResolvedSource,
        title: String?,
    ): Result<Unit>

    fun play()

    fun pause()

    fun playOrPause()

    fun seekTo(seconds: Double)

    fun seekToMillis(positionMs: Long)

    fun forward(seconds: Double = 10.0)

    fun rewind(seconds: Double = 10.0)

    fun stop()

    fun disconnect()
}
