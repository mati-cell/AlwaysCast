package dev.matiribco.alwayscast.feature.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AntiPopupShieldTest {
    private val shield = AntiPopupShield()

    @Test
    fun shouldBlockUrl_conEsquemaMarket_retornaTrue() {
        // Arrange
        val url = "market://details?id=com.shady.adapp"

        // Act
        val result = shield.shouldBlockUrl(url)

        // Assert
        assertTrue(result)
    }

    @Test
    fun shouldBlockUrl_conEsquemaIntent_retornaTrue() {
        // Arrange
        val url = "intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;end"

        // Act
        val result = shield.shouldBlockUrl(url)

        // Assert
        assertTrue(result)
    }

    @Test
    fun shouldBlockUrl_conEsquemaHttpEstandar_retornaFalse() {
        // Arrange
        val url = "http://example.com/video/stream.html"

        // Act
        val result = shield.shouldBlockUrl(url)

        // Assert
        assertFalse(result)
    }

    @Test
    fun shouldBlockUrl_conEsquemaHttpsEstandar_retornaFalse() {
        // Arrange
        val url = "https://example.com/player?id=123"

        // Act
        val result = shield.shouldBlockUrl(url)

        // Assert
        assertFalse(result)
    }

    @Test
    fun shouldBlockUrl_conAboutBlank_retornaFalse() {
        // Arrange
        val url = "about:blank"

        // Act
        val result = shield.shouldBlockUrl(url)

        // Assert
        assertFalse(result)
    }

    @Test
    fun shouldBlockUrl_conEsquemaDesconocido_retornaTrue() {
        // Arrange
        val url = "tel:+123456789"

        // Act
        val result = shield.shouldBlockUrl(url)

        // Assert
        assertTrue(result)
    }

    @Test
    fun getShieldScript_alInvocar_contieneNeutralizacionWindowOpen() {
        // Arrange & Act
        val script = shield.getShieldScript()

        // Assert
        assertTrue(script.contains("window.open"))
    }
}
