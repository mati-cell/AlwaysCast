package dev.matiribco.alwayscast.core.proxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HlsPlaylistRewriterTest {
    private val playlist =
        """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-KEY:METHOD=AES-128,URI="enc.key",IV=0x1
        #EXTINF:4,
        seg0.ts
        #EXTINF:4,
        seg1.ts
        #EXT-X-ENDLIST
        """.trimIndent()

    @Test
    fun rewrite_keyAndSegmentUris_mapsEachResolvedUri() {
        val mappedUris = mutableListOf<String>()
        val rewriter =
            HlsPlaylistRewriter { uri ->
                mappedUris += uri
                uri
            }

        rewriter.rewrite(playlist, "http://cdn.example/dir/index.m3u8")

        assertEquals(
            listOf(
                "http://cdn.example/dir/enc.key",
                "http://cdn.example/dir/seg0.ts",
                "http://cdn.example/dir/seg1.ts",
            ),
            mappedUris,
        )
    }

    @Test
    fun rewrite_mapTag_rewritesInitializationSectionUri() {
        val manifest = "#EXTM3U\n#EXT-X-MAP:URI=\"init.mp4\"\n#EXTINF:4,\nseg0.m4s\n"
        val mappedUris = mutableListOf<String>()
        val rewriter =
            HlsPlaylistRewriter { uri ->
                mappedUris += uri
                uri
            }

        rewriter.rewrite(manifest, "http://cdn.example/dir/index.m3u8")

        assertEquals(listOf("http://cdn.example/dir/init.mp4", "http://cdn.example/dir/seg0.m4s"), mappedUris)
    }

    @Test
    fun rewrite_absoluteSegmentUri_keepsAbsoluteUri() {
        val mappedUris = mutableListOf<String>()
        val rewriter =
            HlsPlaylistRewriter { uri ->
                mappedUris += uri
                uri
            }

        rewriter.rewrite("#EXTM3U\n#EXTINF:4,\nhttps://other.cdn/seg1.ts\n", "http://cdn.example/dir/index.m3u8")

        assertEquals(listOf("https://other.cdn/seg1.ts"), mappedUris)
    }

    @Test
    fun rewrite_plainText_returnsFalseAndTheOriginalText() {
        val plainText = "<html><body>not a playlist</body></html>"
        val rewriter = HlsPlaylistRewriter { uri -> "http://127.0.0.1:9/s/t/$uri" }

        val result = rewriter.rewrite(plainText, "http://cdn.example/dir/index.m3u8")

        assertFalse(result.rewritten)
        assertEquals(plainText, result.text)
    }

    @Test
    fun rewrite_playlist_replacesUrisWithMappedOnes() {
        val rewriter = HlsPlaylistRewriter { uri -> "http://127.0.0.1:9/s/token/${ProxyPath.encode(uri)}" }

        val result = rewriter.rewrite(playlist, "http://cdn.example/dir/index.m3u8")

        assertEquals(3, Regex("http://127\\.0\\.0\\.1:9/s/token/").findAll(result.text).count())
    }
}
