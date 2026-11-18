package dev.matiribco.alwayscast.core.network

import dev.matiribco.alwayscast.core.model.MediaCandidate

interface WebMediaExtractor {
    suspend fun extract(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): List<MediaCandidate>
}
