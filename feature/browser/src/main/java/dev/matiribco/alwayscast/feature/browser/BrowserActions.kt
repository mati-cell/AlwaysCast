package dev.matiribco.alwayscast.feature.browser

import dev.matiribco.alwayscast.core.model.MediaCandidate

data class BrowserActions(
    val onNavigate: (String) -> Unit,
    val onCandidateDetected: (MediaCandidate) -> Unit,
    val onSelectCandidate: (MediaCandidate) -> Unit,
    val onToggleCandidateSheet: () -> Unit,
    val onDismissCandidateSheet: () -> Unit,
)
