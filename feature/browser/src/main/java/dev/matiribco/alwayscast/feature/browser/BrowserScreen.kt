@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package dev.matiribco.alwayscast.feature.browser

import android.webkit.WebView
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.OutlinedButton
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
                onPageStarted = viewModel::onPageStarted,
                onPageFinished = viewModel::onPageFinished,
                onHistoryChanged = viewModel::onHistoryChanged,
            )
        }

    BrowserScreen(
        state = state,
        actions = actions,
        modifier = modifier,
    )
}

@Composable
fun BrowserScreen(
    state: BrowserUiState,
    actions: BrowserActions,
    modifier: Modifier = Modifier,
) {
    var urlInput by remember(state.currentUrl) { mutableStateOf(state.currentUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = state.canGoBack) {
        webViewInstance?.goBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (state.candidates.isNotEmpty()) {
                BrowserFab(
                    candidatesCount = state.candidates.size,
                    onClick = actions.onToggleCandidateSheet,
                )
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
                canGoBack = state.canGoBack,
                onUrlChange = { urlInput = it },
                onNavigate = { actions.onNavigate(urlInput) },
                onGoBack = { webViewInstance?.goBack() },
            )

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            BrowserWebViewContainer(
                currentUrl = state.currentUrl,
                actions = actions,
                onWebViewCreated = { webViewInstance = it },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
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
private fun BrowserWebViewContainer(
    currentUrl: String,
    actions: BrowserActions,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val callbacks =
            remember(actions) {
                BrowserWebViewCallbacks(
                    onPageStarted = actions.onPageStarted,
                    onPageFinished = actions.onPageFinished,
                    onCandidateDetected = actions.onCandidateDetected,
                    onHistoryChanged = actions.onHistoryChanged,
                )
            }

        SafeLifecycleWebView(
            url = currentUrl,
            callbacks = callbacks,
            onWebViewCreated = onWebViewCreated,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BrowserFab(
    candidatesCount: Int,
    onClick: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        val text =
            if (candidatesCount == 1) {
                "1 video detectado"
            } else {
                "$candidatesCount videos detectados"
            }
        Text(text = text)
    }
}

@Composable
private fun BrowserSearchBar(
    url: String,
    canGoBack: Boolean,
    onUrlChange: (String) -> Unit,
    onNavigate: () -> Unit,
    onGoBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canGoBack) {
            OutlinedButton(
                onClick = onGoBack,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Text("◀")
            }
        }

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
