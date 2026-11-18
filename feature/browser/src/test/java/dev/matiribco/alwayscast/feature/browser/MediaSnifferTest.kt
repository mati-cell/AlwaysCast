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
        assertEquals(MediaSniffer.MIME_HLS, candidate?.contentType)
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
        assertEquals(MediaSniffer.MIME_MP4, candidate?.contentType)
        assertEquals(CandidateSource.BROWSER, candidate?.source)
    }

    @Test
    fun sniff_conUrlMkv_retornaMediaCandidateMkv() {
        // Arrange
        val url = "https://cdn.example.com/videos/movie.mkv?auth=abc"

        // Act
        val candidate = sniffer.sniff(url = url)

        // Assert
        assertNotNull(candidate)
        assertEquals(url, candidate?.url)
        assertEquals(MediaSniffer.MIME_MKV, candidate?.contentType)
        assertEquals(CandidateSource.BROWSER, candidate?.source)
    }

    @Test
    fun sniff_conUrlWebm_retornaMediaCandidateWebm() {
        // Arrange
        val url = "https://cdn.example.com/clip.webm"

        // Act
        val candidate = sniffer.sniff(url = url)

        // Assert
        assertNotNull(candidate)
        assertEquals(url, candidate?.url)
        assertEquals(MediaSniffer.MIME_WEBM, candidate?.contentType)
    }

    @Test
    fun sniff_conFalsoPositivoDeJsOImagen_retornaNull() {
        // Arrange
        val jsUrl = "https://cdn.example.com/assets/player.mp4.js"
        val svgUrl = "https://site.com/static/mp4-logo.svg"
        val hosterUrl = "https://mp4upload.com/embed-xyz"

        // Act & Assert
        assertNull(sniffer.sniff(url = jsUrl))
        assertNull(sniffer.sniff(url = svgUrl))
        assertNull(sniffer.sniff(url = hosterUrl))
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
