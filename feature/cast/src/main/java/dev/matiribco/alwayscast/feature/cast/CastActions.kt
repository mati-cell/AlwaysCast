package dev.matiribco.alwayscast.feature.cast

import dev.matiribco.alwayscast.core.model.MediaCandidate

data class CastActions(
    val onUrlChange: (String) -> Unit,
    val onCastClick: () -> Unit,
    val onConnectClick: () -> Unit,
    val onSelectCandidate: (MediaCandidate) -> Unit,
)
