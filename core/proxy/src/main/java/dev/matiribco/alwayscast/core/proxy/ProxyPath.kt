package dev.matiribco.alwayscast.core.proxy

import java.nio.charset.StandardCharsets
import java.util.Base64

object ProxyPath {
    private const val TOKEN_ROOT = "/s"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(upstream: String): String = encoder.encodeToString(upstream.toByteArray(StandardCharsets.UTF_8))

    fun decode(segment: String): String? =
        runCatching { String(decoder.decode(segment), StandardCharsets.UTF_8) }.getOrNull()

    fun forToken(
        token: String,
        upstream: String,
    ): String = "$TOKEN_ROOT/$token/${encode(upstream)}"
}
