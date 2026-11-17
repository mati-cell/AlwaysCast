package dev.matiribco.alwayscast.core.model

data class ResolvedSource(
    val url: String,
    val contentType: String,
)

interface MediaSourceResolver {
    suspend fun resolve(candidate: MediaCandidate): ResolvedSource
}
