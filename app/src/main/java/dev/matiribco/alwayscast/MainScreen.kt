@file:Suppress("ktlint:standard:function-naming", "FunctionNaming")

package dev.matiribco.alwayscast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import dev.matiribco.alwayscast.core.model.MediaCandidate
import dev.matiribco.alwayscast.feature.browser.BrowserRoute
import dev.matiribco.alwayscast.feature.browser.BrowserViewModel
import dev.matiribco.alwayscast.feature.cast.CastRoute
import dev.matiribco.alwayscast.feature.cast.CastViewModel

@Composable
fun MainScreen(
    castViewModel: CastViewModel,
    browserViewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    var currentDestination by remember { mutableStateOf(MainDestination.BROWSER) }

    val onCastCandidate: (MediaCandidate) -> Unit = { candidate ->
        castViewModel.onSelectCandidate(candidate)
        currentDestination = MainDestination.CAST
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentDestination == MainDestination.BROWSER,
                    onClick = { currentDestination = MainDestination.BROWSER },
                    label = { Text("Navegador") },
                    icon = { Text("🌐") },
                )
                NavigationBarItem(
                    selected = currentDestination == MainDestination.CAST,
                    onClick = { currentDestination = MainDestination.CAST },
                    label = { Text("Castear") },
                    icon = { Text("📺") },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
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
}
