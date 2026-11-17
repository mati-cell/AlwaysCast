package dev.matiribco.alwayscast

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import dev.matiribco.alwayscast.core.cast.GoogleCastController
import dev.matiribco.alwayscast.core.designsystem.AlwaysCastTheme
import dev.matiribco.alwayscast.core.network.AndroidMediaInspector
import dev.matiribco.alwayscast.core.network.MediaTypeClassifier
import dev.matiribco.alwayscast.core.proxy.CastProxyServer
import dev.matiribco.alwayscast.core.proxy.FileSpoolStore
import dev.matiribco.alwayscast.feature.cast.CastModule
import dev.matiribco.alwayscast.feature.cast.CastRoute
import dev.matiribco.alwayscast.feature.cast.CastViewModel
import dev.matiribco.alwayscast.feature.cast.LocalProxyResolver
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                requestPermissions(
                    arrayOf(POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE,
                )
            }
        }

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

        val factory =
            CastModule.provideViewModelFactory(
                resolver = resolver,
                controller = controller,
                classifier = MediaTypeClassifier,
                inspector = inspector,
            )

        val viewModel = ViewModelProvider(this, factory)[CastViewModel::class.java]

        setContent {
            AlwaysCastTheme {
                CastRoute(viewModel = viewModel)
            }
        }
    }

    private companion object {
        const val SPOOL_DIR_NAME = "cast_spool"
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    }
}
