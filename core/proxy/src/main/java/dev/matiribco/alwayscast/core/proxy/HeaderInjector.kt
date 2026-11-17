package dev.matiribco.alwayscast.core.proxy

import dev.matiribco.alwayscast.core.model.MediaCandidate
import java.util.Locale

object HeaderInjector {
    const val USER_AGENT = "User-Agent"
    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"

    private val hopByHopHeaders =
        setOf(
            "connection",
            "host",
            "content-length",
            "accept-encoding",
            "transfer-encoding",
        )

    fun headersFor(candidate: MediaCandidate): Map<String, String> {
        val forwarded = candidate.headers.filterKeys { it.lowercase(Locale.ROOT) !in hopByHopHeaders }
        val hasUserAgent = forwarded.keys.any { it.equals(USER_AGENT, ignoreCase = true) }

        return if (hasUserAgent) forwarded else forwarded + (USER_AGENT to DEFAULT_USER_AGENT)
    }
}
