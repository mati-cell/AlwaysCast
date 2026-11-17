package dev.matiribco.alwayscast.core.proxy

import dev.matiribco.alwayscast.core.model.MediaCandidate
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class CastProxyServerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val http = HttpClient.newHttpClient()
    private lateinit var origin: MockWebServer
    private lateinit var server: CastProxyServer

    @Before
    fun setUp() {
        origin = MockWebServer()
        origin.start()
        server =
            CastProxyServer(
                client = OkHttpClient(),
                spool = FileSpoolStore(temporaryFolder.newFolder("spool"), maxBytes = SPOOL_MAX_BYTES),
                token = TOKEN,
            )
        runBlocking { server.start() }
    }

    @After
    fun tearDown() {
        server.stop()
        origin.shutdown()
    }

    @Test
    fun get_wrongToken_returnsNotFound() {
        val path = "/s/wrong-token/${ProxyPath.encode(origin.url("movie.mp4").toString())}"

        val response = get(path)

        assertEquals(404, response.statusCode())
    }

    @Test
    fun get_undecodableSegment_returnsNotFound() {
        val response = get("/s/$TOKEN/!!!not-base64!!!")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun get_openEndedRange_forwardsItAndReturnsPartialContent() {
        origin.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Type", "video/mp4")
                .setHeader("Content-Range", "bytes 4-7/10")
                .setBody("4567"),
        )

        val response = get(proxyPath(origin.url("movie.mp4").toString()), mapOf("Range" to "bytes=4-"))

        assertEquals(206, response.statusCode())
        assertEquals("bytes 4-7/10", response.headers().firstValue("Content-Range").orElse(null))
        assertEquals("bytes=4-", origin.takeRequest().getHeader("Range"))
    }

    @Test
    fun get_rangeIgnoredByOrigin_spoolsOnceAndServesLaterRangesFromTheSpool() {
        origin.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "video/mp4").setBody("0123456789"))
        val upstream = origin.url("movie.mp4").toString()

        val first = get(proxyPath(upstream), mapOf("Range" to "bytes=5-"))
        val second = get(proxyPath(upstream), mapOf("Range" to "bytes=0-3"))

        assertEquals(206, first.statusCode())
        assertEquals("56789", first.body())
        assertEquals(206, second.statusCode())
        assertEquals("0123", second.body())
        assertEquals(1, origin.requestCount)
    }

    @Test
    fun get_playlist_rewritesEveryUriThroughTheProxy() {
        val playlist = "#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"enc.key\"\n#EXTINF:4,\nseg0.ts\n"
        origin.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/vnd.apple.mpegurl")
                .setBody(playlist),
        )

        val response = get(proxyPath(origin.url("dir/index.m3u8").toString()))

        val proxyRoot = "http://127.0.0.1:${server.boundPort}/s/$TOKEN/"
        assertEquals(2, Regex(proxyRoot).findAll(response.body()).count())
    }

    @Test
    fun get_candidateWithReferer_injectsItUpstream() {
        origin.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "video/mp4").setBody("x"))
        val candidate =
            MediaCandidate(
                url = origin.url("movie.mp4").toString(),
                headers = mapOf("Referer" to "https://site.example/watch"),
            )

        val response = get(URI(server.urlFor(candidate)).path, mapOf("Range" to "bytes=0-0"))

        val recorded = origin.takeRequest()
        assertEquals(200, response.statusCode())
        assertEquals("https://site.example/watch", recorded.getHeader("Referer"))
        assertNotNull(recorded.getHeader("User-Agent"))
    }

    @Test
    fun get_signedUrl_upstreamReceivesTheOriginalQuery() {
        origin.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "video/mp4").setBody("x"))
        val candidate = MediaCandidate(url = origin.url("movie.mp4?token=a%2Bb%2Fc&exp=17").toString())

        get(URI(server.urlFor(candidate)).path)

        assertEquals("a+b/c", origin.takeRequest().requestUrl?.queryParameter("token"))
    }

    @Test
    fun getAllowedUrl_reportsTheProxyBaseWithTheToken() {
        val candidate = MediaCandidate(url = "https://cdn.example/movie.mp4")

        val url = server.urlFor(candidate)

        assertTrue(url.startsWith("http://127.0.0.1:${server.boundPort}/s/$TOKEN/"))
    }

    private fun proxyPath(upstream: String): String = ProxyPath.forToken(TOKEN, upstream)

    private fun get(path: String, headers: Map<String, String> = emptyMap()): HttpResponse<String> {
        val requestBuilder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:${server.boundPort}$path"))
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        return http.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private companion object {
        const val TOKEN = "test-token"
        const val SPOOL_MAX_BYTES = 1_024L * 1_024L
    }
}
