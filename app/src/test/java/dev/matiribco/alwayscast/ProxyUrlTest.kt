package dev.matiribco.alwayscast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyUrlTest {

    @Test
    fun format_validHostPortAndPath_returnsCompleteHttpUrl() {
        val url = ProxyUrl.format(
            host = LAN_HOST,
            port = TEST_PORT,
            path = TEST_PATH,
        )

        assertEquals("http://192.168.1.50:8080/s/token/video.mp4", url)
    }

    @Test(expected = IllegalArgumentException::class)
    fun format_invalidPort_throwsIllegalArgumentException() {
        ProxyUrl.format(
            host = LAN_HOST,
            port = INVALID_PORT,
            path = TEST_PATH,
        )
    }

    @Test
    fun isLoopback_loopbackAddresses_returnsTrue() {
        assertTrue(ProxyUrl.isLoopback("127.0.0.1"))
        assertTrue(ProxyUrl.isLoopback("localhost"))
        assertTrue(ProxyUrl.isLoopback("::1"))
    }

    @Test
    fun isLoopback_lanIpAddress_returnsFalse() {
        assertFalse(ProxyUrl.isLoopback(LAN_HOST))
    }

    private companion object {
        const val LAN_HOST = "192.168.1.50"
        const val TEST_PORT = 8080
        const val INVALID_PORT = 0
        const val TEST_PATH = "/s/token/video.mp4"
    }
}
