package dev.matiribco.alwayscast.core.model

sealed interface Verdict {
    data class Supported(
        val contentType: String,
    ) : Verdict

    data class Unsupported(
        val reason: UnsupportedReason,
    ) : Verdict

    data class Unknown(
        val contentType: String?,
    ) : Verdict
}

enum class UnsupportedReason {
    DRM,
    HEVC,
    AV1,
    MSE_BLOB,
    NO_MEDIA,
}
