package dev.matiribco.alwayscast.core.network

import dev.matiribco.alwayscast.core.model.UnsupportedReason
import dev.matiribco.alwayscast.core.model.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTypeClassifierTest {

    @Test
    fun classify_mp4WithoutContentType_returnsSupportedMp4() {
        val url = "https://cdn.example/movie.mp4"

        val verdict = MediaTypeClassifier.classify(url, null)

        assertEquals(Verdict.Supported("video/mp4"), verdict)
    }

    @Test
    fun classify_mkvWithoutContentType_returnsSupportedMatroska() {
        val url = "https://cdn.example/movie.mkv"

        val verdict = MediaTypeClassifier.classify(url, null)

        assertEquals(Verdict.Supported("video/x-matroska"), verdict)
    }

    @Test
    fun classify_hlsPlaylist_returnsSupportedHls() {
        val url = "https://cdn.example/movie/index.m3u8"

        val verdict = MediaTypeClassifier.classify(url, null)

        assertEquals(Verdict.Supported("application/x-mpegURL"), verdict)
    }

    @Test
    fun classify_webmWithoutContentType_returnsSupportedWebm() {
        val url = "https://cdn.example/movie.webm"

        val verdict = MediaTypeClassifier.classify(url, null)

        assertEquals(Verdict.Supported("video/webm"), verdict)
    }

    @Test
    fun classify_mp3WithoutContentType_returnsSupportedMp3() {
        val url = "https://cdn.example/song.mp3"

        val verdict = MediaTypeClassifier.classify(url, null)

        assertEquals(Verdict.Supported("audio/mpeg"), verdict)
    }

    @Test
    fun classify_flacWithoutContentType_returnsSupportedFlac() {
        val url = "https://cdn.example/song.flac"

        val verdict = MediaTypeClassifier.classify(url, null)

        assertEquals(Verdict.Supported("audio/flac"), verdict)
    }

    @Test
    fun classify_blobUrl_returnsUnsupportedMseBlob() {
        val url = "blob:https://cdn.example/8f1c0d2e"

        val verdict = MediaTypeClassifier.classify(url, null)

        assertEquals(Verdict.Unsupported(UnsupportedReason.MSE_BLOB), verdict)
    }

    @Test
    fun classify_hevcCodecHint_returnsUnsupportedHevc() {
        val contentType = "video/mp4; codecs=\"hvc1.1.6.L93.B0\""

        val verdict = MediaTypeClassifier.classify("https://cdn.example/movie.mp4", contentType)

        assertEquals(Verdict.Unsupported(UnsupportedReason.HEVC), verdict)
    }

    @Test
    fun classify_hevcExtension_returnsUnsupportedHevc() {
        val url = "https://cdn.example/movie.hevc.mp4"

        val verdict = MediaTypeClassifier.classify(url, null)

        assertEquals(Verdict.Unsupported(UnsupportedReason.HEVC), verdict)
    }

    @Test
    fun classify_av1CodecHint_returnsUnsupportedAv1() {
        val contentType = "video/mp4; codecs=\"av01.0.04M.08\""

        val verdict = MediaTypeClassifier.classify("https://cdn.example/movie.mp4", contentType)

        assertEquals(Verdict.Unsupported(UnsupportedReason.AV1), verdict)
    }

    @Test
    fun classify_widevineHint_returnsUnsupportedDrm() {
        val contentType = "video/mp4; protection=widevine"

        val verdict = MediaTypeClassifier.classify("https://cdn.example/movie.mp4", contentType)

        assertEquals(Verdict.Unsupported(UnsupportedReason.DRM), verdict)
    }

    @Test
    fun classify_unknownExtensionWithoutContentType_returnsUnknown() {
        val url = "https://cdn.example/stream"

        val verdict = MediaTypeClassifier.classify(url, null)

        assertEquals(Verdict.Unknown(null), verdict)
    }

    @Test
    fun classify_unknownVideoContentType_returnsUnknownWithThatType() {
        val contentType = "video/vnd.example"

        val verdict = MediaTypeClassifier.classify("https://cdn.example/stream", contentType)

        assertEquals(Verdict.Unknown(contentType), verdict)
    }

    @Test
    fun classify_knownContentTypeWithParameters_returnsNormalizedType() {
        val contentType = "video/mp4; codecs=\"avc1.42E01E, mp4a.40.2\""

        val verdict = MediaTypeClassifier.classify("https://cdn.example/stream", contentType)

        assertEquals(Verdict.Supported("video/mp4"), verdict)
    }

    @Test
    fun classify_uppercaseExtensionWithQueryString_returnsSupportedMp4() {
        val url = "https://cdn.example/MOVIE.MP4?token=a%2Bb%2Fc&exp=17"

        val verdict = MediaTypeClassifier.classify(url, null)

        assertEquals(Verdict.Supported("video/mp4"), verdict)
    }
}
