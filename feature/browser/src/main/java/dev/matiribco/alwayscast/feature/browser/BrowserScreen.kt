@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package dev.matiribco.alwayscast.feature.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.matiribco.alwayscast.core.model.MediaCandidate

@Composable
fun BrowserRoute(
    viewModel: BrowserViewModel,
    onCastCandidate: (MediaCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel, onCastCandidate) {
            BrowserActions(
                onNavigate = viewModel::onNavigate,
                onCandidateDetected = viewModel::onCandidateDetected,
                onSelectCandidate = onCastCandidate,
                onToggleCandidateSheet = viewModel::onToggleCandidateSheet,
                onDismissCandidateSheet = viewModel::onDismissCandidateSheet,
            )
        }

    BrowserScreen(
        state = state,
        actions = actions,
        onPageStarted = viewModel::onPageStarted,
        onPageFinished = viewModel::onPageFinished,
        modifier = modifier,
    )
}

@Composable
fun BrowserScreen(
    state: BrowserUiState,
    actions: BrowserActions,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var urlInput by remember(state.currentUrl) { mutableStateOf(state.currentUrl) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (state.candidates.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = actions.onToggleCandidateSheet,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    val count = state.candidates.size
                    val text = if (count == 1) "1 video detectado" else "$count videos detectados"
                    Text(text = text)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            BrowserSearchBar(
                url = urlInput,
                onUrlChange = { urlInput = it },
                onNavigate = { actions.onNavigate(urlInput) },
            )

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val callbacks =
                    remember(actions, onPageStarted, onPageFinished) {
                        BrowserWebViewCallbacks(
                            onPageStarted = onPageStarted,
                            onPageFinished = onPageFinished,
                            onCandidateDetected = actions.onCandidateDetected,
                        )
                    }

                SafeLifecycleWebView(
                    url = state.currentUrl,
                    callbacks = callbacks,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (state.isCandidateSheetVisible) {
            CandidateBottomSheet(
                candidates = state.candidates,
                onSelectCandidate = actions.onSelectCandidate,
                onDismiss = actions.onDismissCandidateSheet,
            )
        }
    }
}

@Composable
private fun BrowserSearchBar(
    url: String,
    onUrlChange: (String) -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = { Text("https:// o búsqueda...") },
            singleLine = true,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
        )

        Button(onClick = onNavigate) {
            Text("Ir")
        }
    }
}
