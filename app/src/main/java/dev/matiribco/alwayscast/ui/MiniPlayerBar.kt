@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package dev.matiribco.alwayscast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.matiribco.alwayscast.core.model.PlaybackState

@Composable
fun MiniPlayerBar(
    playback: PlaybackState,
    onPlayOrPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 6.dp,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MiniPlayerProgressBar(playback = playback)

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "📺", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(12.dp))
                MiniPlayerInfo(playback = playback, modifier = Modifier.weight(1f))
                MiniPlayerPlayButton(playback = playback, onPlayOrPause = onPlayOrPause)
            }
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
    playback: PlaybackState,
    modifier: Modifier = Modifier,
) {
    if (!playback.isLive && playback.durationMs > 0L) {
        LinearProgressIndicator(
            progress = { playback.progressFraction },
            modifier = modifier.fillMaxWidth().height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun MiniPlayerInfo(
    playback: PlaybackState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = playback.title ?: "Transmitiendo a TV",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val timeText =
            if (playback.isLive) {
                "En vivo"
            } else if (playback.durationMs > 0L) {
                "${playback.formattedPosition} / ${playback.formattedDuration}"
            } else {
                "Reproduciendo"
            }

        Text(
            text = timeText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun MiniPlayerPlayButton(
    playback: PlaybackState,
    onPlayOrPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onPlayOrPause,
        modifier = modifier.size(40.dp),
    ) {
        if (playback.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else if (playback.isPlaying) {
            Text("⏸", style = MaterialTheme.typography.titleMedium)
        } else {
            Text("▶", style = MaterialTheme.typography.titleMedium)
        }
    }
}
