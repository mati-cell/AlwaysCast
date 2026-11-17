package dev.matiribco.alwayscast.feature.cast

import dev.matiribco.alwayscast.core.model.MediaCandidate
import dev.matiribco.alwayscast.core.model.MediaSourceResolver
import dev.matiribco.alwayscast.core.model.ResolvedSource
import dev.matiribco.alwayscast.core.model.Verdict
import dev.matiribco.alwayscast.core.network.MediaClassifier
import dev.matiribco.alwayscast.core.proxy.CastProxyServer

class LocalProxyResolver(
    private val proxy: CastProxyServer,
    private val classifier: MediaClassifier,
) : MediaSourceResolver {
    override suspend fun resolve(candidate: MediaCandidate): ResolvedSource {
        if (proxy.boundPort == 0) {
            proxy.start()
        }
        val verdict = classifier.classify(candidate.url, candidate.contentType)
        val contentType =
            when (verdict) {
                is Verdict.Supported -> verdict.contentType
                is Verdict.Unknown -> candidate.contentType ?: DEFAULT_CONTENT_TYPE
                is Verdict.Unsupported -> candidate.contentType ?: DEFAULT_CONTENT_TYPE
            }
        val proxyUrl = proxy.urlFor(candidate)
        return ResolvedSource(url = proxyUrl, contentType = contentType)
    }

    private companion object {
        const val DEFAULT_CONTENT_TYPE = "application/octet-stream"
    }
}
