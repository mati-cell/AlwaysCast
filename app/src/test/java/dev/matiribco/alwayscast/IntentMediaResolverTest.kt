package dev.matiribco.alwayscast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntentMediaResolverTest {
    @Test
    fun extractMediaUrl_actionSendWithDirectUrl_returnsUrl() {
        val action = "android.intent.action.SEND"
        val text = "https://ok.ru/video/7715958885030"

        val result = IntentMediaResolver.extractMediaUrl(action = action, dataString = null, extraText = text)

        assertEquals("https://ok.ru/video/7715958885030", result)
    }

    @Test
    fun extractMediaUrl_actionSendWithTextSurroundingUrl_extractsOnlyUrl() {
        val action = "android.intent.action.SEND"
        val text = "Mira esta pelicula: https://ok.ru/video/7715958885030 en AlwaysCast!"

        val result = IntentMediaResolver.extractMediaUrl(action = action, dataString = null, extraText = text)

        assertEquals("https://ok.ru/video/7715958885030", result)
    }

    @Test
    fun extractMediaUrl_actionViewWithDataUri_returnsDataUri() {
        val action = "android.intent.action.VIEW"
        val dataUri = "https://cdn.example.com/stream.mp4"

        val result = IntentMediaResolver.extractMediaUrl(action = action, dataString = dataUri, extraText = null)

        assertEquals("https://cdn.example.com/stream.mp4", result)
    }

    @Test
    fun extractMediaUrl_unrelatedAction_returnsNull() {
        val action = "android.intent.action.MAIN"

        val result = IntentMediaResolver.extractMediaUrl(action = action, dataString = null, extraText = null)

        assertNull(result)
    }
}
