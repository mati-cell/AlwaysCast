package dev.matiribco.alwayscast.feature.cast

import dev.matiribco.alwayscast.core.model.CandidateSource
import dev.matiribco.alwayscast.core.model.MediaCandidate
import dev.matiribco.alwayscast.core.model.MediaSourceResolver
import dev.matiribco.alwayscast.core.model.ResolvedSource
import dev.matiribco.alwayscast.core.model.Verdict
import dev.matiribco.alwayscast.core.network.MediaClassifier
import dev.matiribco.alwayscast.core.proxy.CastProxyServer

class LocalProxyResolver(
    private val proxy: CastProxyServer,
    private val classifier: MediaClassifier,
    private val forceProxy: Boolean = false,
) : MediaSourceResolver {
    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource {
        val verdict = classifier.classify(candidate.url, candidate.contentType)
        val contentType =
            when (verdict) {
                is Verdict.Supported -> verdict.contentType
                is Verdict.Unknown -> candidate.contentType ?: DEFAULT_CONTENT_TYPE
                is Verdict.Unsupported -> candidate.contentType ?: DEFAULT_CONTENT_TYPE
            }

        val targetUrl =
            if (!forceProxy && canBypassProxy(candidate)) {
                candidate.url
            } else {
                if (proxy.boundPort == 0) {
                    proxy.start()
                }
                proxy.urlFor(candidate)
            }

        return ResolvedSource(url = targetUrl, contentType = contentType)
    }

    private fun canBypassProxy(candidate: MediaCandidate): Boolean {
        if (candidate.source == CandidateSource.LOCAL_FILE || candidate.headers.isNotEmpty()) {
            return false
        }
        val lower = candidate.url.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }

    private companion object {
        const val DEFAULT_CONTENT_TYPE = "application/octet-stream"
    }
}
