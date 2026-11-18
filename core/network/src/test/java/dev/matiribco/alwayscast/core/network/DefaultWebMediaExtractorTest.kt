package dev.matiribco.alwayscast.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultWebMediaExtractorTest {
    private lateinit var server: MockWebServer
    private lateinit var extractor: WebMediaExtractor

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        extractor = DefaultWebMediaExtractor(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun extract_directMediaResponse_returnsSingleCandidate() =
        runBlocking {
            // Arrange
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "video/mp4")
                    .setBody("dummy mp4 bytes"),
            )
            val url = server.url("/movie.mp4").toString()

            // Act
            val candidates = extractor.extract(url)

            // Assert
            assertEquals(1, candidates.size)
            assertEquals(url, candidates.first().url)
            assertEquals("video/mp4", candidates.first().contentType)
        }

    @Test
    fun extract_okRuPage_extractsHlsAndMp4Qualities() =
        runBlocking {
            // Arrange
            val htmlWithOkOptions =
                """
                <!DOCTYPE html>
                <html>
                <body>
                <div class="vp_video">
                    <div data-module="OKVideo" data-options="{&quot;flashvars&quot;:{&quot;metadata&quot;:{&quot;hlsManifestUrl&quot;:&quot;https://vd582.okcdn.ru/video.m3u8?sig=123&quot;,&quot;videos&quot;:[{&quot;name&quot;:&quot;hd&quot;,&quot;url&quot;:&quot;https://vd582.okcdn.ru/hd.mp4&quot;},{&quot;name&quot;:&quot;sd&quot;,&quot;url&quot;:&quot;https://vd582.okcdn.ru/sd.mp4&quot;}]}}}">
                    </div>
                </div>
                </body>
                </html>
                """.trimIndent()

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/html; charset=UTF-8")
                    .setBody(htmlWithOkOptions),
            )
            val pageUrl = server.url("/video/7715958885030").toString()

            // Act
            val candidates = extractor.extract(pageUrl)

            // Assert
            assertEquals(3, candidates.size)
            val hls = candidates[0]
            assertEquals("https://vd582.okcdn.ru/video.m3u8?sig=123", hls.url)
            assertEquals("application/x-mpegURL", hls.contentType)
            assertTrue(hls.label?.contains("HLS", ignoreCase = true) == true)

            val hd = candidates[1]
            assertEquals("https://vd582.okcdn.ru/hd.mp4", hd.url)
            assertEquals("video/mp4", hd.contentType)
            assertTrue(hd.label?.contains("HD", ignoreCase = true) == true)

            val sd = candidates[2]
            assertEquals("https://vd582.okcdn.ru/sd.mp4", sd.url)
            assertEquals("video/mp4", sd.contentType)
            assertTrue(sd.label?.contains("SD", ignoreCase = true) == true)
        }

    @Test
    fun extract_genericHtmlWithVideoTags_extractsCandidates() =
        runBlocking {
            // Arrange
            val htmlWithVideo =
                """
                <!DOCTYPE html>
                <html>
                <body>
                    <video src="https://example.com/direct.mp4"></video>
                    <source src="/streams/master.m3u8" type="application/x-mpegURL">
                </body>
                </html>
                """.trimIndent()

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/html")
                    .setBody(htmlWithVideo),
            )
            val pageUrl = server.url("/watch").toString()

            // Act
            val candidates = extractor.extract(pageUrl)

            // Assert
            assertEquals(2, candidates.size)
            assertTrue(candidates.any { it.url == "https://example.com/direct.mp4" })
            val relativeHls = candidates.first { it.url.endsWith("master.m3u8") }
            assertEquals(server.url("/streams/master.m3u8").toString(), relativeHls.url)
        }

    @Test
    fun extract_emptyHtml_returnsEmptyList() =
        runBlocking {
            // Arrange
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/html")
                    .setBody("<html><body><p>No video here</p></body></html>"),
            )
            val pageUrl = server.url("/novideo").toString()

            // Act
            val candidates = extractor.extract(pageUrl)

            // Assert
            assertTrue(candidates.isEmpty())
        }
}
