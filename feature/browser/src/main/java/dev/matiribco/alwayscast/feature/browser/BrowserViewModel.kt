package dev.matiribco.alwayscast.feature.browser

import androidx.lifecycle.ViewModel
import dev.matiribco.alwayscast.core.model.MediaCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BrowserViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    fun onNavigate(url: String) {
        _uiState.update { it.copy(currentUrl = url) }
    }

    fun onPageStarted(url: String) {
        _uiState.update {
            it.copy(
                currentUrl = url,
                isLoading = true,
                candidates = emptyList(),
            )
        }
    }

    fun onPageFinished(
        url: String,
        title: String? = null,
    ) {
        _uiState.update {
            it.copy(
                currentUrl = url,
                isLoading = false,
                pageTitle = title ?: it.pageTitle,
            )
        }
    }

    fun onHistoryChanged(
        canGoBack: Boolean,
        canGoForward: Boolean,
    ) {
        _uiState.update {
            it.copy(
                canGoBack = canGoBack,
                canGoForward = canGoForward,
            )
        }
    }

    fun onCandidateDetected(candidate: MediaCandidate) {
        _uiState.update { current ->
            if (current.candidates.any { it.url == candidate.url }) {
                current
            } else {
                current.copy(candidates = current.candidates + candidate)
            }
        }
    }

    fun onToggleCandidateSheet() {
        _uiState.update { it.copy(isCandidateSheetVisible = !it.isCandidateSheetVisible) }
    }

    fun onDismissCandidateSheet() {
        _uiState.update { it.copy(isCandidateSheetVisible = false) }
    }

    fun onClearCandidates() {
        _uiState.update { it.copy(candidates = emptyList()) }
    }
}
