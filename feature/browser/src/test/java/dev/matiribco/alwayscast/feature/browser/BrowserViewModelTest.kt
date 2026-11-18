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

    @Test
    fun onHistoryChanged_conValores_actualizaEstadoDeNavegacion() {
        // Arrange & Assert initial state
        assertFalse(viewModel.uiState.value.canGoBack)
        assertFalse(viewModel.uiState.value.canGoForward)

        // Act
        viewModel.onHistoryChanged(canGoBack = true, canGoForward = false)

        // Assert
        assertTrue(viewModel.uiState.value.canGoBack)
        assertFalse(viewModel.uiState.value.canGoForward)
    }

    @Test
    fun normalizeUrl_conEsquema_retornaMismaUrl() {
        assertEquals("https://ok.ru/video/123", viewModel.normalizeUrl("https://ok.ru/video/123"))
        assertEquals("http://example.com/test", viewModel.normalizeUrl("http://example.com/test"))
    }

    @Test
    fun normalizeUrl_sinEsquemaConDominio_agregaHttps() {
        assertEquals("https://ok.ru", viewModel.normalizeUrl("ok.ru"))
        assertEquals("https://www.google.com", viewModel.normalizeUrl("www.google.com"))
    }

    @Test
    fun normalizeUrl_conTextoDeBusqueda_retornaBusquedaEnGoogle() {
        assertEquals(
            "https://www.google.com/search?q=pelicula+accion",
            viewModel.normalizeUrl("pelicula accion"),
        )
        assertEquals(
            "https://www.google.com/search?q=matrix",
            viewModel.normalizeUrl("matrix"),
        )
    }

    @Test
    fun normalizeUrl_conTextoVacio_retornaVacio() {
        assertEquals("", viewModel.normalizeUrl("   "))
    }

    @Test
    fun onGoHome_restableceEstadoAValoresIniciales() {
        // Arrange
        viewModel.onNavigate("https://example.com")
        viewModel.onCandidateDetected(MediaCandidate(url = "https://example.com/video.mp4"))
        viewModel.onToggleCandidateSheet()

        // Act
        viewModel.onGoHome()

        // Assert
        val state = viewModel.uiState.value
        assertEquals("", state.currentUrl)
        assertEquals("", state.pageTitle)
        assertFalse(state.canGoBack)
        assertFalse(state.isCandidateSheetVisible)
        assertTrue(state.candidates.isEmpty())
    }
}
