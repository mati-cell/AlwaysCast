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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.ImeAction
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
                onGoHome = viewModel::onGoHome,
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
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = state.canGoBack || state.currentUrl.isNotBlank()) {
        if (state.canGoBack) {
            webViewInstance?.goBack()
        } else {
            actions.onGoHome()
        }
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
        BrowserContent(
            state = state,
            actions = actions,
            onWebViewCreated = { webViewInstance = it },
            onGoBack = { webViewInstance?.goBack() },
            modifier = Modifier.padding(innerPadding),
        )

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
private fun BrowserContent(
    state: BrowserUiState,
    actions: BrowserActions,
    onWebViewCreated: (WebView) -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var urlInput by remember(state.currentUrl) { mutableStateOf(state.currentUrl) }

    Column(modifier = modifier.fillMaxSize()) {
        val searchBarState =
            SearchBarState(
                url = urlInput,
                canGoBack = state.canGoBack,
                isBrowsing = state.currentUrl.isNotBlank(),
            )
        val searchBarActions =
            SearchBarActions(
                onUrlChange = { urlInput = it },
                onNavigate = { actions.onNavigate(urlInput) },
                onGoBack = onGoBack,
                onGoHome = actions.onGoHome,
            )

        BrowserSearchBar(
            state = searchBarState,
            actions = searchBarActions,
        )

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (state.currentUrl.isBlank()) {
            BrowserHomeScreen(
                onNavigate = actions.onNavigate,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            BrowserWebViewContainer(
                currentUrl = state.currentUrl,
                actions = actions,
                onWebViewCreated = onWebViewCreated,
                modifier = Modifier.weight(1f).fillMaxWidth(),
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

private data class SearchBarState(
    val url: String,
    val canGoBack: Boolean,
    val isBrowsing: Boolean,
)

private data class SearchBarActions(
    val onUrlChange: (String) -> Unit,
    val onNavigate: () -> Unit,
    val onGoBack: () -> Unit,
    val onGoHome: () -> Unit,
)

@Composable
private fun BrowserSearchBar(
    state: SearchBarState,
    actions: SearchBarActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.canGoBack) {
            OutlinedButton(
                onClick = actions.onGoBack,
                modifier = Modifier.padding(end = 6.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
            }
        }

        if (state.isBrowsing) {
            OutlinedButton(
                onClick = actions.onGoHome,
                modifier = Modifier.padding(end = 6.dp),
            ) {
                Icon(Icons.Rounded.Home, contentDescription = "Inicio")
            }
        }

        OutlinedTextField(
            value = state.url,
            onValueChange = actions.onUrlChange,
            placeholder = { Text("https:// o búsqueda...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { actions.onNavigate() }),
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
        )

        Button(onClick = actions.onNavigate) {
            Text("Ir")
        }
    }
}
