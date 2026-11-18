package dev.matiribco.alwayscast.feature.browser

import dev.matiribco.alwayscast.core.model.MediaCandidate

data class BrowserWebViewCallbacks(
    val onPageStarted: (String) -> Unit,
    val onPageFinished: (String, String?) -> Unit,
    val onCandidateDetected: (MediaCandidate) -> Unit,
    val onProgressChanged: (Int) -> Unit = {},
)
