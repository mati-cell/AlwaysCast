package dev.matiribco.alwayscast.core.network

import dev.matiribco.alwayscast.core.model.UnsupportedReason
import dev.matiribco.alwayscast.core.model.Verdict
import java.util.Locale

object ProbeVerdicts {
    private val unsupportedVideoMimes =
        mapOf(
            "video/hevc" to UnsupportedReason.HEVC,
            "video/av01" to UnsupportedReason.AV1,
        )

    private val supportedVideoMimes =
        setOf(
            "video/avc",
            "video/x-vnd.on2.vp8",
            "video/x-vnd.on2.vp9",
            "video/mp4v-es",
            "video/3gpp",
        )

    fun fromCodecs(
        videoMime: String?,
        containerContentType: String?,
    ): Verdict {
        val normalizedMime = videoMime?.lowercase(Locale.ROOT)
        val unsupportedReason = normalizedMime?.let(unsupportedVideoMimes::get)

        return when {
            unsupportedReason != null -> Verdict.Unsupported(unsupportedReason)
            normalizedMime == null || normalizedMime in supportedVideoMimes ->
                containerContentType?.let(Verdict::Supported) ?: Verdict.Unknown(null)
            else -> Verdict.Unknown(containerContentType)
        }
    }
}
