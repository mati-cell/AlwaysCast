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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import dev.matiribco.alwayscast.core.designsystem.AlwaysCastTokens
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
        shape = RoundedCornerShape(AlwaysCastTokens.Radius.card),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AlwaysCastTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = playback.title ?: "Transmitiendo al televisor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(AlwaysCastTokens.Spacing.md))

            PlaybackTimeline(playback = playback, onSeekTo = actions.onSeekTo)

            Spacer(modifier = Modifier.height(AlwaysCastTokens.Spacing.lg))

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
                    style = AlwaysCastTokens.Typography.TabularTime,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = playback.formattedDuration,
                    style = AlwaysCastTokens.Typography.TabularTime,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    } else if (playback.isLive) {
        LiveBadge(modifier = modifier)
    }
}

@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    Surface(
        color = AlwaysCastTokens.Palette.SemanticLive.copy(alpha = 0.15f),
        shape = RoundedCornerShape(AlwaysCastTokens.Radius.full),
        modifier = modifier.padding(vertical = AlwaysCastTokens.Spacing.sm),
    ) {
        Text(
            text = "● EN VIVO",
            style = MaterialTheme.typography.labelSmall,
            color = AlwaysCastTokens.Palette.SemanticLive,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier.padding(
                    horizontal = AlwaysCastTokens.Spacing.md,
                    vertical = AlwaysCastTokens.Spacing.xs,
                ),
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
        horizontalArrangement = Arrangement.spacedBy(AlwaysCastTokens.Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = actions.onRewind,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Replay10,
                contentDescription = "Retroceder 10 segundos",
                modifier = Modifier.size(24.dp),
            )
        }

        PlaybackPlayPauseButton(
            playback = playback,
            onPlayOrPause = actions.onPlayOrPause,
        )

        FilledTonalIconButton(
            onClick = actions.onForward,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Forward10,
                contentDescription = "Adelantar 10 segundos",
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(AlwaysCastTokens.Spacing.xs))

        OutlinedIconButton(
            onClick = actions.onStop,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Stop,
                contentDescription = "Detener transmisión",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PlaybackPlayPauseButton(
    playback: PlaybackState,
    onPlayOrPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onPlayOrPause,
        modifier = modifier.size(64.dp),
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
            Icon(
                imageVector = Icons.Rounded.Pause,
                contentDescription = "Pausar",
                modifier = Modifier.size(32.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Reproducir",
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
