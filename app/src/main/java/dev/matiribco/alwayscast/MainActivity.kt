package dev.matiribco.alwayscast

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import dev.matiribco.alwayscast.core.cast.GoogleCastController
import dev.matiribco.alwayscast.core.designsystem.AlwaysCastTheme
import dev.matiribco.alwayscast.core.network.AndroidMediaInspector
import dev.matiribco.alwayscast.core.network.DefaultWebMediaExtractor
import dev.matiribco.alwayscast.core.network.MediaTypeClassifier
import dev.matiribco.alwayscast.core.proxy.CastProxyServer
import dev.matiribco.alwayscast.core.proxy.FileSpoolStore
import dev.matiribco.alwayscast.feature.browser.BrowserViewModel
import dev.matiribco.alwayscast.feature.cast.CastModule
import dev.matiribco.alwayscast.feature.cast.CastViewModel
import dev.matiribco.alwayscast.feature.cast.LocalProxyResolver
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity() {
    private var castViewModel: CastViewModel? = null
    private var browserViewModel: BrowserViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()

        val httpClient = OkHttpClient()
        val spoolDir = cacheDir.resolve(SPOOL_DIR_NAME).apply { mkdirs() }
        val spool = FileSpoolStore(spoolDir)
        val lanIp = LanAddressResolver.findLanIp()

        val proxyServer =
            CastForegroundService.activeProxy
                ?: CastProxyServer(
                    client = httpClient,
                    spool = spool,
                    host = lanIp,
                )

        val resolver =
            LocalProxyResolver(
                proxy = proxyServer,
                classifier = MediaTypeClassifier,
            )

        val controller = GoogleCastController(this)
        val inspector = AndroidMediaInspector(httpClient)
        val extractor = DefaultWebMediaExtractor(httpClient)

        val factory =
            CastModule.provideViewModelFactory(
                resolver = resolver,
                controller = controller,
                classifier = MediaTypeClassifier,
                inspector = inspector,
                extractor = extractor,
            )

        val cViewModel = ViewModelProvider(this, factory)[CastViewModel::class.java]
        val bViewModel = ViewModelProvider(this)[BrowserViewModel::class.java]
        castViewModel = cViewModel
        browserViewModel = bViewModel

        handleIncomingIntent(intent, cViewModel, bViewModel)

        setContent {
            AlwaysCastTheme {
                MainScreen(
                    castViewModel = cViewModel,
                    browserViewModel = bViewModel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val cvm = castViewModel
        val bvm = browserViewModel
        if (cvm != null && bvm != null) {
            handleIncomingIntent(intent, cvm, bvm)
        }
    }

    private fun handleIncomingIntent(
        intent: Intent?,
        cViewModel: CastViewModel,
        bViewModel: BrowserViewModel,
    ) {
        val url = IntentMediaResolver.extractMediaUrl(intent)
        if (!url.isNullOrBlank()) {
            if (isDirectMediaUrl(url)) {
                cViewModel.onUrlChange(url)
                cViewModel.onCastClick()
            } else {
                bViewModel.onNavigate(url)
            }
        }
    }

    private fun isDirectMediaUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".mp4") ||
            lower.endsWith(".m3u8") ||
            lower.endsWith(".webm") ||
            lower.endsWith(".mkv")
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                requestPermissions(
                    arrayOf(POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE,
                )
            }
        }
    }

    private companion object {
        const val SPOOL_DIR_NAME = "cast_spool"
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    }
}
