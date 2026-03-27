package com.vmate.downloader.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.vmate.downloader.R
import com.vmate.downloader.core.download.DownloadManager
import com.vmate.downloader.domain.models.Download

class DownloadForegroundService : Service() {

    private lateinit var downloadManager: DownloadManager
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        downloadManager = DownloadManager(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val download = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DOWNLOAD, Download::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DOWNLOAD)
                }
                download?.let {
                    startForeground(NOTIFICATION_ID, buildNotification("Starting download: ${it.filename}"))
                    downloadManager.startDownload(it)
                }
            }
            ACTION_CANCEL -> {
                val id = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (id != -1L) downloadManager.cancelDownload(id)
            }
            ACTION_STOP_ALL -> {
                downloadManager.cancelAll()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        downloadManager.cancelAll()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
    }

    companion object {
        const val ACTION_START = "com.vmate.downloader.ACTION_START"
        const val ACTION_CANCEL = "com.vmate.downloader.ACTION_CANCEL"
        const val ACTION_STOP_ALL = "com.vmate.downloader.ACTION_STOP_ALL"
        const val EXTRA_DOWNLOAD = "extra_download"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001
    }
}