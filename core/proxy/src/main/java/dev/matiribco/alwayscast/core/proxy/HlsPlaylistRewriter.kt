package dev.matiribco.alwayscast.core.proxy

import java.net.URI

data class RewrittenPlaylist(
    val text: String,
    val rewritten: Boolean,
)

class HlsPlaylistRewriter(
    private val mapUri: (String) -> String,
) {
    fun rewrite(
        playlist: String,
        baseUrl: String,
    ): RewrittenPlaylist {
        if (!playlist.trimStart().startsWith(PLAYLIST_HEADER)) {
            return RewrittenPlaylist(text = playlist, rewritten = false)
        }

        val lines =
            playlist.lineSequence().map { line ->
                when {
                    line.startsWith(KEY_TAG) || line.startsWith(MAP_TAG) -> rewriteUriAttribute(line, baseUrl)
                    line.isBlank() || line.startsWith(COMMENT_PREFIX) -> line
                    else -> mapUri(resolve(baseUrl, line.trim()))
                }
            }

        return RewrittenPlaylist(text = lines.joinToString(separator = "\n"), rewritten = true)
    }

    private fun rewriteUriAttribute(
        line: String,
        baseUrl: String,
    ): String {
        val attributeStart = line.indexOf(URI_ATTRIBUTE)
        val valueStart = attributeStart + URI_ATTRIBUTE.length
        val valueEnd = line.indexOf(ATTRIBUTE_CLOSING, valueStart)

        return when {
            attributeStart < 0 || valueEnd < 0 -> line
            else -> {
                val resolvedUri = resolve(baseUrl, line.substring(valueStart, valueEnd))
                line.replaceRange(valueStart, valueEnd, mapUri(resolvedUri))
            }
        }
    }

    private fun resolve(
        baseUrl: String,
        uri: String,
    ): String = runCatching { URI(baseUrl).resolve(uri).toString() }.getOrDefault(uri)

    private companion object {
        const val PLAYLIST_HEADER = "#EXTM3U"
        const val KEY_TAG = "#EXT-X-KEY:"
        const val MAP_TAG = "#EXT-X-MAP:"
        const val COMMENT_PREFIX = "#"
        const val URI_ATTRIBUTE = "URI=\""
        const val ATTRIBUTE_CLOSING = '"'
    }
}
