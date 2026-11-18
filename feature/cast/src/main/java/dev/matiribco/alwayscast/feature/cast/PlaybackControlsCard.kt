@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package dev.matiribco.alwayscast.feature.cast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.matiribco.alwayscast.core.model.PlaybackState

@Composable
fun PlaybackControlsCard(
    playback: PlaybackState,
    actions: CastActions,
    modifier: Modifier = Modifier,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = playback.title ?: "Transmitiendo al televisor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlaybackTimeline(playback = playback, onSeekTo = actions.onSeekTo)

            Spacer(modifier = Modifier.height(16.dp))

            PlaybackControlButtons(playback = playback, actions = actions)
        }
    }
}

@Composable
private fun PlaybackTimeline(
    playback: PlaybackState,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isDragging) dragFraction else playback.progressFraction
    val displayPositionMs =
        if (isDragging && playback.durationMs > 0L) {
            (dragFraction * playback.durationMs).toLong()
        } else {
            playback.positionMs
        }

    if (!playback.isLive && playback.durationMs > 0L) {
        Column(modifier = modifier.fillMaxWidth()) {
            Slider(
                value = currentFraction,
                onValueChange = {
                    isDragging = true
                    dragFraction = it
                },
                onValueChangeFinished = {
                    val targetMs = (dragFraction * playback.durationMs).toLong()
                    onSeekTo(targetMs)
                    isDragging = false
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = PlaybackState.formatDuration(displayPositionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = playback.formattedDuration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    } else if (playback.isLive) {
        Text(
            text = "● EN VIVO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun PlaybackControlButtons(
    playback: PlaybackState,
    actions: CastActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = actions.onRewind,
            modifier = Modifier.size(48.dp),
        ) {
            Text("-10s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }

        FilledIconButton(
            onClick = actions.onPlayOrPause,
            modifier = Modifier.size(64.dp),
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        ) {
            if (playback.isBuffering) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                )
            } else if (playback.isPlaying) {
                Text("⏸", style = MaterialTheme.typography.headlineMedium)
            } else {
                Text("▶", style = MaterialTheme.typography.headlineMedium)
            }
        }

        FilledTonalIconButton(
            onClick = actions.onForward,
            modifier = Modifier.size(48.dp),
        ) {
            Text("+10s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedIconButton(
            onClick = actions.onStop,
            modifier = Modifier.size(44.dp),
        ) {
            Text("⏹", style = MaterialTheme.typography.titleMedium)
        }
    }
}
