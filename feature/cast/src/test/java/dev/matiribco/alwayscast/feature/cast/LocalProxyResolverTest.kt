package dev.matiribco.alwayscast.feature.cast

import dev.matiribco.alwayscast.core.model.CandidateSource
import dev.matiribco.alwayscast.core.model.MediaCandidate
import dev.matiribco.alwayscast.core.model.Verdict
import dev.matiribco.alwayscast.core.network.MediaClassifier
import dev.matiribco.alwayscast.core.proxy.CastProxyServer
import dev.matiribco.alwayscast.core.proxy.FileSpoolStore
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files

class LocalProxyResolverTest {
    private lateinit var tempDir: java.io.File
    private lateinit var spool: FileSpoolStore
    private lateinit var client: OkHttpClient
    private lateinit var proxy: CastProxyServer

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("test-spool").toFile()
        spool = FileSpoolStore(tempDir)
        client = OkHttpClient()
        proxy = CastProxyServer(client, spool)
    }

    @After
    fun tearDown() {
        proxy.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun resolve_candidatoSinHeadersNiProxyForzado_retornaUrlDirectaYNoIniciaProxy() =
        runTest {
            // Arrange
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Supported(MIME_MP4)
                }
            val resolver = LocalProxyResolver(proxy, fakeClassifier, forceProxy = false)
            val candidate = MediaCandidate(url = UPSTREAM_URL)

            // Act
            val resolved = resolver.resolve(candidate)

            // Assert
            assertEquals(UPSTREAM_URL, resolved.url)
            assertEquals(MIME_MP4, resolved.contentType)
            assertEquals(0, proxy.boundPort)
        }

    @Test
    fun resolve_candidatoConHeaders_iniciaProxyYRetornaUrlProxied() =
        runTest {
            // Arrange
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Supported(MIME_MP4)
                }
            val resolver = LocalProxyResolver(proxy, fakeClassifier)
            val candidate =
                MediaCandidate(
                    url = UPSTREAM_URL,
                    headers = mapOf("Referer" to "https://example.com"),
                )

            // Act
            val resolved = resolver.resolve(candidate)

            // Assert
            assertTrue(proxy.boundPort > 0)
            assertTrue(resolved.url.startsWith("http://127.0.0.1:${proxy.boundPort}"))
            assertEquals(MIME_MP4, resolved.contentType)
        }

    @Test
    fun resolve_candidatoConProxyForzado_iniciaProxyYRetornaUrlProxied() =
        runTest {
            // Arrange
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Supported(MIME_MP4)
                }
            val resolver = LocalProxyResolver(proxy, fakeClassifier, forceProxy = true)
            val candidate = MediaCandidate(url = UPSTREAM_URL)

            // Act
            val resolved = resolver.resolve(candidate)

            // Assert
            assertTrue(proxy.boundPort > 0)
            assertTrue(resolved.url.startsWith("http://127.0.0.1:${proxy.boundPort}"))
            assertEquals(MIME_MP4, resolved.contentType)
        }

    @Test
    fun resolve_candidatoArchivoLocal_iniciaProxyYRetornaUrlProxied() =
        runTest {
            // Arrange
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Supported(MIME_MP4)
                }
            val resolver = LocalProxyResolver(proxy, fakeClassifier)
            val candidate =
                MediaCandidate(
                    url = "file:///storage/emulated/0/video.mp4",
                    source = CandidateSource.LOCAL_FILE,
                )

            // Act
            val resolved = resolver.resolve(candidate)

            // Assert
            assertTrue(proxy.boundPort > 0)
            assertTrue(resolved.url.startsWith("http://127.0.0.1:${proxy.boundPort}"))
            assertEquals(MIME_MP4, resolved.contentType)
        }

    @Test
    fun resolve_formatoDesconocidoSinContentType_retornaOctetStream() =
        runTest {
            // Arrange
            val fakeClassifier =
                object : MediaClassifier {
                    override fun classify(
                        url: String,
                        contentType: String?,
                    ): Verdict = Verdict.Unknown(null)
                }
            val resolver = LocalProxyResolver(proxy, fakeClassifier, forceProxy = false)
            val candidate = MediaCandidate(url = UPSTREAM_URL)

            // Act
            val resolved = resolver.resolve(candidate)

            // Assert
            assertEquals(UPSTREAM_URL, resolved.url)
            assertEquals(OCTET_STREAM, resolved.contentType)
        }

    private companion object {
        const val UPSTREAM_URL = "http://example.com/video.mp4"
        const val MIME_MP4 = "video/mp4"
        const val OCTET_STREAM = "application/octet-stream"
    }
}
