package dev.matiribco.alwayscast.core.proxy

import dev.matiribco.alwayscast.core.model.MediaCandidate
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import okhttp3.OkHttpClient
import java.io.Closeable
import java.security.SecureRandom
import java.util.Base64

class CastProxyServer(
    client: OkHttpClient,
    spool: SpoolStore,
    private val token: String = randomToken(),
    private val host: String = LOOPBACK,
) : Closeable {
    private val forwarder = UpstreamForwarder(client, spool, ::proxyUrlFor)
    private var engine: EmbeddedServer<*, *>? = null
    private var port: Int = 0

    val boundPort: Int
        get() = port

    suspend fun start() {
        val server = embeddedServer(CIO, port = 0, host = BIND_HOST) { installRoutes() }
        server.start(wait = false)
        var resolved = server.engine.resolvedConnectors()
        var retries = 0
        while (resolved.isEmpty() && retries < MAX_CONNECT_RETRIES) {
            kotlinx.coroutines.delay(RETRY_DELAY_MILLIS)
            resolved = server.engine.resolvedConnectors()
            retries++
        }
        port = resolved.firstOrNull()?.port ?: DEFAULT_PORT
        engine = server
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 0, timeoutMillis = STOP_TIMEOUT_MILLIS)
        engine = null
    }

    override fun close() {
        stop()
    }

    fun urlFor(candidate: MediaCandidate): String {
        forwarder.register(candidate)
        return proxyUrlFor(candidate.url)
    }

    private fun proxyUrlFor(upstream: String): String = "http://$host:$boundPort${ProxyPath.forToken(token, upstream)}"

    private fun Application.installRoutes() {
        routing {
            get(ROUTE) {
                val upstream = call.parameters[SEGMENT_PARAMETER]?.let(ProxyPath::decode)
                val tokenMatches = call.parameters[TOKEN_PARAMETER] == token

                if (upstream != null && tokenMatches) {
                    forwarder.forward(call, upstream)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }

    private companion object {
        const val BIND_HOST = "0.0.0.0"
        const val LOOPBACK = "127.0.0.1"
        const val ROUTE = "/s/{token}/{segment}"
        const val TOKEN_PARAMETER = "token"
        const val SEGMENT_PARAMETER = "segment"
        const val STOP_TIMEOUT_MILLIS = 1_000L
        const val MAX_CONNECT_RETRIES = 10
        const val RETRY_DELAY_MILLIS = 50L
        const val DEFAULT_PORT = 8080
    }
}

private const val TOKEN_BYTES = 16

private fun randomToken(): String {
    val bytes = ByteArray(TOKEN_BYTES)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
