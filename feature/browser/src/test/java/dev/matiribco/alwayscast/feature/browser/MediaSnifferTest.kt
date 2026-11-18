package dev.matiribco.alwayscast.feature.browser

import dev.matiribco.alwayscast.core.model.CandidateSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MediaSnifferTest {
    private val sniffer = MediaSniffer()

    @Test
    fun sniff_conUrlM3u8_retornaMediaCandidateHls() {
        // Arrange
        val url = "https://cdn.example.com/hls/master.m3u8?token=xyz"

        // Act
        val candidate = sniffer.sniff(url = url)

        // Assert
        assertNotNull(candidate)
        assertEquals(url, candidate?.url)
        assertEquals("application/x-mpegURL", candidate?.contentType)
        assertEquals(CandidateSource.BROWSER, candidate?.source)
    }

    @Test
    fun sniff_conUrlMp4_retornaMediaCandidateMp4() {
        // Arrange
        val url = "https://cdn.example.com/videos/movie_1080p.mp4"

        // Act
        val candidate = sniffer.sniff(url = url)

        // Assert
        assertNotNull(candidate)
        assertEquals(url, candidate?.url)
        assertEquals("video/mp4", candidate?.contentType)
        assertEquals(CandidateSource.BROWSER, candidate?.source)
    }

    @Test
    fun sniff_conContentTypeVideo_retornaMediaCandidate() {
        // Arrange
        val url = "https://cdn.example.com/stream/get?id=456"
        val contentType = "video/mp4"

        // Act
        val candidate = sniffer.sniff(url = url, contentType = contentType)

        // Assert
        assertNotNull(candidate)
        assertEquals(contentType, candidate?.contentType)
    }

    @Test
    fun sniff_conUrlHtmlNormal_retornaNull() {
        // Arrange
        val url = "https://example.com/watch/popular-movies"

        // Act
        val candidate = sniffer.sniff(url = url, contentType = "text/html")

        // Assert
        assertNull(candidate)
    }

    @Test
    fun sniff_conHeaders_preservaRefererYUserAgent() {
        // Arrange
        val url = "https://cdn.example.com/stream.m3u8"
        val headers =
            mapOf(
                "Referer" to "https://example.com/watch",
                "User-Agent" to "Mozilla/5.0 Android",
            )

        // Act
        val candidate = sniffer.sniff(url = url, headers = headers)

        // Assert
        assertNotNull(candidate)
        assertEquals("https://example.com/watch", candidate?.headers?.get("Referer"))
        assertEquals("Mozilla/5.0 Android", candidate?.headers?.get("User-Agent"))
    }
}
