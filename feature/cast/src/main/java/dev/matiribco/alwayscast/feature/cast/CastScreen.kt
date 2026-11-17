@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package dev.matiribco.alwayscast.feature.cast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

@Composable
fun CastRoute(
    viewModel: CastViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CastScreen(
        state = state,
        onUrlChange = viewModel::onUrlChange,
        onCastClick = viewModel::onCastClick,
        onConnectClick = viewModel::onConnectClick,
        modifier = modifier,
    )
}

@Composable
fun CastScreen(
    state: CastUiState,
    onUrlChange: (String) -> Unit,
    onCastClick: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var textInput by remember { mutableStateOf(state.url) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CastHeader()

            DeviceStatusCard(deviceName = state.deviceName)

            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    onUrlChange(it)
                },
                label = { Text("URL del video web") },
                placeholder = { Text("https://servidor.com/video.mp4") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            CastActionButtons(
                isCastEnabled = textInput.isNotBlank() && state.phase != Phase.PROBING,
                onCastClick = onCastClick,
                onConnectClick = onConnectClick,
            )

            PhaseIndicator(state = state)

            if (!state.message.isNullOrBlank()) {
                MessageCard(
                    message = state.message,
                    isError = state.phase == Phase.ERROR || state.phase == Phase.UNSUPPORTED,
                )
            }
        }
    }
}

@Composable
private fun CastHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "AlwaysCast",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        CastMediaRouteButton()
    }
}

@Composable
private fun CastActionButtons(
    isCastEnabled: Boolean,
    onCastClick: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onCastClick,
            enabled = isCastEnabled,
            modifier = Modifier.weight(1f),
        ) {
            Text("Castear")
        }

        OutlinedButton(
            onClick = onConnectClick,
            modifier = Modifier.weight(1f),
        ) {
            Text("Conectar")
        }
    }
}

@Composable
fun CastMediaRouteButton(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            MediaRouteButton(context).apply {
                CastButtonFactory.setUpMediaRouteButton(context, this)
            }
        },
        modifier = modifier.size(40.dp),
    )
}

@Composable
private fun DeviceStatusCard(
    deviceName: String?,
    modifier: Modifier = Modifier,
) {
    val isConnected = deviceName != null
    val backgroundColor =
        if (isConnected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val textColor =
        if (isConnected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    if (isConnected) {
                        "Dispositivo: $deviceName"
                    } else {
                        "Sin conectar (toca el icono de Cast arriba para elegir tu Chromecast)"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
        }
    }
}

@Composable
private fun PhaseIndicator(
    state: CastUiState,
    modifier: Modifier = Modifier,
) {
    when (state.phase) {
        Phase.IDLE -> Unit
        Phase.PROBING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier.padding(vertical = 8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(
                    text = "Sondeando formato y códecs...",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Phase.READY -> {
            Text(
                text = "Medio compatible. Listo para iniciar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier,
            )
        }
        Phase.CASTING -> {
            Text(
                text = "Transmitiendo video al Chromecast...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = modifier,
            )
        }
        Phase.UNSUPPORTED -> Unit
        Phase.ERROR -> Unit
    }
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    val contentColor =
        if (isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}
