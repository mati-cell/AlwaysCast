package dev.matiribco.alwayscast.core.network

import dev.matiribco.alwayscast.core.model.MediaCandidate
import dev.matiribco.alwayscast.core.model.Verdict

interface MediaInspector {
    suspend fun inspect(candidate: MediaCandidate): Verdict
}
