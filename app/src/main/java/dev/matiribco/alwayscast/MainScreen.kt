@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package dev.matiribco.alwayscast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.matiribco.alwayscast.core.model.MediaCandidate
import dev.matiribco.alwayscast.feature.browser.BrowserRoute
import dev.matiribco.alwayscast.feature.browser.BrowserViewModel
import dev.matiribco.alwayscast.feature.cast.CastRoute
import dev.matiribco.alwayscast.feature.cast.CastViewModel
import dev.matiribco.alwayscast.ui.MiniPlayerBar

@Composable
fun MainScreen(
    castViewModel: CastViewModel,
    browserViewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    var currentDestination by remember { mutableStateOf(MainDestination.BROWSER) }
    val castState by castViewModel.state.collectAsStateWithLifecycle()

    val onCastCandidate: (MediaCandidate) -> Unit = { candidate ->
        castViewModel.onSelectCandidate(candidate)
        currentDestination = MainDestination.CAST
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AlwaysCastBottomBar(
                castState = castState,
                currentDestination = currentDestination,
                onPlayOrPause = castViewModel::onPlayOrPause,
                onSelectDestination = { currentDestination = it },
            )
        },
    ) { innerPadding ->
        MainContentHost(
            currentDestination = currentDestination,
            browserViewModel = browserViewModel,
            castViewModel = castViewModel,
            onCastCandidate = onCastCandidate,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun AlwaysCastBottomBar(
    castState: dev.matiribco.alwayscast.feature.cast.CastUiState,
    currentDestination: MainDestination,
    onPlayOrPause: () -> Unit,
    onSelectDestination: (MainDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (castState.playback.hasActiveMedia && currentDestination != MainDestination.CAST) {
            MiniPlayerBar(
                playback = castState.playback,
                onPlayOrPause = onPlayOrPause,
                onClick = { onSelectDestination(MainDestination.CAST) },
            )
        }
        NavigationBar {
            NavigationBarItem(
                selected = currentDestination == MainDestination.BROWSER,
                onClick = { onSelectDestination(MainDestination.BROWSER) },
                label = { Text("Navegador") },
                icon = { Icon(Icons.Rounded.Language, contentDescription = "Navegador") },
            )
            NavigationBarItem(
                selected = currentDestination == MainDestination.CAST,
                onClick = { onSelectDestination(MainDestination.CAST) },
                label = { Text("Castear") },
                icon = { Icon(Icons.Rounded.Tv, contentDescription = "Castear") },
            )
        }
    }
}

@Composable
private fun MainContentHost(
    currentDestination: MainDestination,
    browserViewModel: BrowserViewModel,
    castViewModel: CastViewModel,
    onCastCandidate: (MediaCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        val isBrowser = currentDestination == MainDestination.BROWSER
        Box(
            modifier =
                if (isBrowser) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.size(0.dp)
                },
        ) {
            BrowserRoute(
                viewModel = browserViewModel,
                onCastCandidate = onCastCandidate,
            )
        }

        if (currentDestination == MainDestination.CAST) {
            CastRoute(viewModel = castViewModel)
        }
    }
}
