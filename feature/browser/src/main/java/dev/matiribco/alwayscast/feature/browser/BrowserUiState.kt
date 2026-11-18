package dev.matiribco.alwayscast.feature.browser

import dev.matiribco.alwayscast.core.model.MediaCandidate

data class BrowserUiState(
    val currentUrl: String = "",
    val isLoading: Boolean = false,
    val pageTitle: String = "",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val candidates: List<MediaCandidate> = emptyList(),
    val isCandidateSheetVisible: Boolean = false,
)
