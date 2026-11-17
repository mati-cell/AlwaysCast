package dev.matiribco.alwayscast.core.proxy

import dev.matiribco.alwayscast.core.model.MediaCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HeaderInjectorTest {
    @Test
    fun headersFor_candidateWithRefererAndCookie_keepsBoth() {
        val candidate =
            MediaCandidate(
                url = "https://cdn.example/v.mp4",
                headers = mapOf("Referer" to "https://site.example/page", "Cookie" to "session=abc"),
            )

        val headers = HeaderInjector.headersFor(candidate)

        assertEquals("https://site.example/page", headers["Referer"])
        assertEquals("session=abc", headers["Cookie"])
    }

    @Test
    fun headersFor_candidateWithoutUserAgent_addsBrowserUserAgent() {
        val candidate = MediaCandidate(url = "https://cdn.example/v.mp4")

        val headers = HeaderInjector.headersFor(candidate)

        assertFalse(headers["User-Agent"].isNullOrBlank())
    }

    @Test
    fun headersFor_candidateWithUserAgent_keepsTheGivenOne() {
        val candidate =
            MediaCandidate(
                url = "https://cdn.example/v.mp4",
                headers = mapOf("User-Agent" to "CustomAgent/1.0"),
            )

        val headers = HeaderInjector.headersFor(candidate)

        assertEquals("CustomAgent/1.0", headers["User-Agent"])
    }

    @Test
    fun headersFor_candidateWithHopByHopHeaders_dropsThem() {
        val candidate =
            MediaCandidate(
                url = "https://cdn.example/v.mp4",
                headers =
                    mapOf(
                        "Connection" to "keep-alive",
                        "Host" to "cdn.example",
                        "Content-Length" to "10",
                        "Accept-Encoding" to "gzip",
                    ),
            )

        val headers = HeaderInjector.headersFor(candidate)

        assertFalse(headers.keys.any { it.lowercase() in HOP_BY_HOP_HEADERS })
    }

    private companion object {
        val HOP_BY_HOP_HEADERS = setOf("connection", "host", "content-length", "accept-encoding")
    }
}
