package dev.matiribco.alwayscast.core.model

data class MediaCandidate(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val contentType: String? = null,
    val source: CandidateSource = CandidateSource.PASTED,
)

enum class CandidateSource {
    PASTED,
    SHARED_INTENT,
    BROWSER,
    LOCAL_FILE,
}
