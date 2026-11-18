package dev.matiribco.alwayscast.feature.browser

import dev.matiribco.alwayscast.core.model.CandidateSource
import dev.matiribco.alwayscast.core.model.MediaCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserViewModelTest {
    private val viewModel = BrowserViewModel()

    @Test
    fun onNavigate_conUrlValida_actualizaCurrentUrl() {
        // Arrange
        val url = "https://example.com"

        // Act
        viewModel.onNavigate(url)

        // Assert
        assertEquals(url, viewModel.uiState.value.currentUrl)
    }

    @Test
    fun onCandidateDetected_conNuevoCandidato_agregaALista() {
        // Arrange
        val candidate =
            MediaCandidate(
                url = "https://example.com/video.mp4",
                contentType = "video/mp4",
                source = CandidateSource.BROWSER,
            )

        // Act
        viewModel.onCandidateDetected(candidate)

        // Assert
        assertEquals(1, viewModel.uiState.value.candidates.size)
        assertEquals(
            candidate,
            viewModel.uiState.value.candidates
                .first(),
        )
    }

    @Test
    fun onCandidateDetected_conCandidatoDuplicado_noAgregaDuplicado() {
        // Arrange
        val candidate =
            MediaCandidate(
                url = "https://example.com/video.mp4",
                contentType = "video/mp4",
                source = CandidateSource.BROWSER,
            )

        // Act
        viewModel.onCandidateDetected(candidate)
        viewModel.onCandidateDetected(candidate.copy(label = "Other"))

        // Assert
        assertEquals(1, viewModel.uiState.value.candidates.size)
    }

    @Test
    fun onClearCandidates_alInvocar_limpiaLista() {
        // Arrange
        val candidate =
            MediaCandidate(
                url = "https://example.com/video.mp4",
                contentType = "video/mp4",
                source = CandidateSource.BROWSER,
            )
        viewModel.onCandidateDetected(candidate)

        // Act
        viewModel.onClearCandidates()

        // Assert
        assertTrue(
            viewModel.uiState.value.candidates
                .isEmpty(),
        )
    }

    @Test
    fun onToggleCandidateSheet_alInvocar_cambiaVisibilidad() {
        // Arrange & Assert initial state
        assertFalse(viewModel.uiState.value.isCandidateSheetVisible)

        // Act
        viewModel.onToggleCandidateSheet()

        // Assert
        assertTrue(viewModel.uiState.value.isCandidateSheetVisible)

        // Act again
        viewModel.onToggleCandidateSheet()

        // Assert
        assertFalse(viewModel.uiState.value.isCandidateSheetVisible)
    }

    @Test
    fun onPageStarted_conNuevaUrl_limpiaCandidatosPreviosYMarcaLoading() {
        // Arrange
        val oldCandidate =
            MediaCandidate(
                url = "https://old.com/video.mp4",
                contentType = "video/mp4",
                source = CandidateSource.BROWSER,
            )
        viewModel.onCandidateDetected(oldCandidate)
        val newUrl = "https://newsite.com"

        // Act
        viewModel.onPageStarted(newUrl)

        // Assert
        assertTrue(
            viewModel.uiState.value.candidates
                .isEmpty(),
        )
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(newUrl, viewModel.uiState.value.currentUrl)
    }
}
