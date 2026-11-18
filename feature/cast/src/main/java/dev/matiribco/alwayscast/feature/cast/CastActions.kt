package dev.matiribco.alwayscast.feature.cast

import dev.matiribco.alwayscast.core.model.MediaCandidate

data class CastActions(
    val onUrlChange: (String) -> Unit,
    val onCastClick: () -> Unit,
    val onConnectClick: () -> Unit,
    val onSelectCandidate: (MediaCandidate) -> Unit,
    val onPlayOrPause: () -> Unit = {},
    val onSeekTo: (Long) -> Unit = {},
    val onForward: () -> Unit = {},
    val onRewind: () -> Unit = {},
    val onStop: () -> Unit = {},
)
