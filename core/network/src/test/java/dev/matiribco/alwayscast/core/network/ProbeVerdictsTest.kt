package dev.matiribco.alwayscast.core.network

import dev.matiribco.alwayscast.core.model.UnsupportedReason
import dev.matiribco.alwayscast.core.model.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class ProbeVerdictsTest {

    @Test
    fun fromCodecs_hevcVideo_returnsUnsupportedHevc() {
        val verdict = ProbeVerdicts.fromCodecs(videoMime = "video/hevc", containerContentType = "video/mp4")

        assertEquals(Verdict.Unsupported(UnsupportedReason.HEVC), verdict)
    }

    @Test
    fun fromCodecs_av1Video_returnsUnsupportedAv1() {
        val verdict = ProbeVerdicts.fromCodecs(videoMime = "video/av01", containerContentType = "video/mp4")

        assertEquals(Verdict.Unsupported(UnsupportedReason.AV1), verdict)
    }

    @Test
    fun fromCodecs_h264Video_returnsSupportedContainer() {
        val verdict = ProbeVerdicts.fromCodecs(videoMime = "video/avc", containerContentType = "video/mp4")

        assertEquals(Verdict.Supported("video/mp4"), verdict)
    }

    @Test
    fun fromCodecs_audioOnlyFile_returnsSupportedContainer() {
        val verdict = ProbeVerdicts.fromCodecs(videoMime = null, containerContentType = "audio/mpeg")

        assertEquals(Verdict.Supported("audio/mpeg"), verdict)
    }

    @Test
    fun fromCodecs_unknownVideoCodec_returnsUnknownWithContainer() {
        val verdict = ProbeVerdicts.fromCodecs(videoMime = "video/x-vendor", containerContentType = "video/mp4")

        assertEquals(Verdict.Unknown("video/mp4"), verdict)
    }

    @Test
    fun fromCodecs_noVideoAndNoContainer_returnsUnknownWithoutType() {
        val verdict = ProbeVerdicts.fromCodecs(videoMime = null, containerContentType = null)

        assertEquals(Verdict.Unknown(null), verdict)
    }

    @Test
    fun fromCodecs_vp9VideoInMatroska_returnsSupportedMatroska() {
        val verdict = ProbeVerdicts.fromCodecs(videoMime = "video/x-vnd.on2.vp9", containerContentType = "video/x-matroska")

        assertEquals(Verdict.Supported("video/x-matroska"), verdict)
    }
}
