@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package dev.matiribco.alwayscast.feature.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BrowserHomeScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrowserHomeHeader()
        Spacer(modifier = Modifier.height(24.dp))
        BrowserQuickShortcuts(onNavigate = onNavigate, clipboard = clipboardManager)
        Spacer(modifier = Modifier.height(24.dp))
        BrowserFeatureHighlights()
    }
}

@Composable
private fun BrowserHomeHeader() {
    Spacer(modifier = Modifier.height(16.dp))
    Icon(
        imageVector = Icons.Rounded.Language,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Navegador AlwaysCast",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text =
            "Navega o busca videos en la web. Cuando comience la reproducción, se detectará " +
                "automáticamente para transmitirlo a tu Chromecast.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrowserQuickShortcuts(
    onNavigate: (String) -> Unit,
    clipboard: ClipboardManager,
) {
    Text(
        text = "Accesos rápidos",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(12.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = { onNavigate("https://ok.ru") }) {
            Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("OK.ru")
        }
        OutlinedButton(onClick = { onNavigate("https://www.google.com") }) {
            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Google")
        }
        OutlinedButton(onClick = { onNavigate("https://vimeo.com/watch") }) {
            Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Vimeo")
        }
        OutlinedButton(
            onClick = {
                val clip = clipboard.getText()?.text
                if (!clip.isNullOrBlank()) {
                    onNavigate(clip)
                }
            },
        ) {
            Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pegar enlace")
        }
    }
}

@Composable
private fun BrowserFeatureHighlights() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🛡️ Escudo Anti-Popups Activo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    "Las ventanas emergentes y redirecciones no deseadas son neutralizadas " +
                        "para una navegación limpia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "⚡ Detección Dinámica",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    "Al reproducir contenido multimedia en cualquier sitio web, un botón flotante " +
                        "te permitirá elegir calidad y castear al instante.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
