package dev.matiribco.alwayscast.core.cast

import dev.matiribco.alwayscast.core.model.CastState
import dev.matiribco.alwayscast.core.model.ResolvedSource
import kotlinx.coroutines.flow.Flow
import kotlin.Result

interface CastController {
    val state: Flow<CastState>

    suspend fun connect(): Result<String>

    suspend fun load(
        source: ResolvedSource,
        title: String?,
    ): Result<Unit>

    fun playOrPause()

    fun seekTo(seconds: Double)

    fun stop()

    fun disconnect()
}
