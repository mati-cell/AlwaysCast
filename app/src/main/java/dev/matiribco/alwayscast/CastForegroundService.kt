package dev.matiribco.alwayscast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dev.matiribco.alwayscast.core.proxy.CastProxyServer
import dev.matiribco.alwayscast.core.proxy.FileSpoolStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class CastForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var proxyServer: CastProxyServer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundServiceWithNotification()
                startProxyIfNeeded()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.launch {
            proxyServer?.close()
            activeProxy = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundServiceWithNotification() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startProxyIfNeeded() {
        if (proxyServer != null) return

        serviceScope.launch {
            runCatching {
                val spoolDir = cacheDir.resolve(SPOOL_DIR_NAME).apply { mkdirs() }
                val spool = FileSpoolStore(spoolDir)
                val client = OkHttpClient()

                val server =
                    CastProxyServer(
                        client = client,
                        spool = spool,
                        hostProvider = LanAddressResolver::findLanIp,
                    )
                server.start()
                proxyServer = server
                activeProxy = server
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = CHANNEL_DESCRIPTION
                }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification
                .Builder(this, CHANNEL_ID)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(NOTIFICATION_TEXT)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification
                .Builder(this)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(NOTIFICATION_TEXT)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build()
        }

    companion object {
        const val ACTION_START = "dev.matiribco.alwayscast.action.START_PROXY"
        const val ACTION_STOP = "dev.matiribco.alwayscast.action.STOP_PROXY"

        private const val CHANNEL_ID = "alwayscast_proxy_channel"
        private const val CHANNEL_NAME = "AlwaysCast Proxy"
        private const val CHANNEL_DESCRIPTION = "Mantiene el proxy local activo para servir video al Chromecast"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_TITLE = "AlwaysCast"
        private const val NOTIFICATION_TEXT = "AlwaysCast está sirviendo el video"
        private const val SPOOL_DIR_NAME = "cast_spool"

        @Volatile
        var activeProxy: CastProxyServer? = null
            private set

        fun start(context: Context) {
            val intent =
                Intent(context, CastForegroundService::class.java).apply {
                    action = ACTION_START
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent =
                Intent(context, CastForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
            context.startService(intent)
        }
    }
}
