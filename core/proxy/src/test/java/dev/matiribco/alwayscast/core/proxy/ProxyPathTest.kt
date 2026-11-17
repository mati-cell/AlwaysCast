package dev.matiribco.alwayscast.core.proxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ProxyPathTest {
    @Test
    fun decode_encodedSignedUrl_returnsTheOriginalUrl() {
        val upstreamUrl = "https://cdn.example/v.mp4?token=a+b/c=d&exp=17"

        val decodedUrl = ProxyPath.decode(ProxyPath.encode(upstreamUrl))

        assertEquals(upstreamUrl, decodedUrl)
    }

    @Test
    fun encode_urlWithSlashes_returnsSegmentWithoutPaddingOrSlashes() {
        val segment = ProxyPath.encode("https://cdn.example/v.mp4")

        assertFalse(segment.contains('/'))
        assertFalse(segment.contains('='))
    }

    @Test
    fun forToken_upstreamUrl_returnsTokenRootedPath() {
        val expectedPath = "/s/abc/${ProxyPath.encode("https://cdn.example/a.mp4")}"

        val path = ProxyPath.forToken("abc", "https://cdn.example/a.mp4")

        assertEquals(expectedPath, path)
    }

    @Test
    fun decode_segmentWithInvalidBase64_returnsNull() {
        val decodedUrl = ProxyPath.decode("!!!not-base64!!!")

        assertNull(decodedUrl)
    }

    @Test
    fun decode_segmentWithUtf8Url_returnsTheOriginalUrl() {
        val upstreamUrl = "https://cdn.example/ñandú/video.mp4?name=película"

        val decodedUrl = ProxyPath.decode(ProxyPath.encode(upstreamUrl))

        assertEquals(upstreamUrl, decodedUrl)
    }
}
