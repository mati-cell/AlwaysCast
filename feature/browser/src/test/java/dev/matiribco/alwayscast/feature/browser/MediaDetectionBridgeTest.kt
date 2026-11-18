package dev.matiribco.alwayscast.feature.browser

import dev.matiribco.alwayscast.core.model.MediaCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MediaDetectionBridgeTest {
    private val sniffer = MediaSniffer()
    private var detectedCandidate: MediaCandidate? = null
    private val bridge =
        MediaDetectionBridge(
            sniffer = sniffer,
            onCandidateDetected = { detectedCandidate = it },
        )

    @Test
    fun onMediaDetected_conUrlValida_emiteCandidato() {
        // Arrange
        val url = "https://cdn.example.com/playlist.m3u8"

        // Act
        bridge.onMediaDetected(url, "application/x-mpegURL")

        // Assert
        assertNotNull(detectedCandidate)
        assertEquals(url, detectedCandidate?.url)
    }

    @Test
    fun onMediaDetected_conUrlNoMedia_noEmiteCandidato() {
        // Arrange
        val url = "https://example.com/index.html"

        // Act
        bridge.onMediaDetected(url, "text/html")

        // Assert
        assertNull(detectedCandidate)
    }
}
