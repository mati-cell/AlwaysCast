package dev.matiribco.alwayscast.feature.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.matiribco.alwayscast.core.cast.CastController
import dev.matiribco.alwayscast.core.model.MediaSourceResolver
import dev.matiribco.alwayscast.core.network.MediaClassifier
import dev.matiribco.alwayscast.core.network.MediaInspector
import dev.matiribco.alwayscast.core.network.MediaTypeClassifier
import dev.matiribco.alwayscast.core.network.WebMediaExtractor

object CastModule {
    fun provideViewModelFactory(
        resolver: MediaSourceResolver,
        controller: CastController,
        classifier: MediaClassifier = MediaTypeClassifier,
        inspector: MediaInspector? = null,
        extractor: WebMediaExtractor? = null,
    ): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(CastViewModel::class.java)) {
                    "Unknown ViewModel class: ${modelClass.name}"
                }
                return CastViewModel(
                    classifier = classifier,
                    resolver = resolver,
                    controller = controller,
                    inspector = inspector,
                    extractor = extractor,
                ) as T
            }
        }
}
