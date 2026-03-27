package com.vmate.downloader.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CANCEL -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                val serviceIntent = Intent(context, DownloadForegroundService::class.java).apply {
                    action = DownloadForegroundService.ACTION_CANCEL
                    putExtra(DownloadForegroundService.EXTRA_DOWNLOAD_ID, downloadId)
                }
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        const val ACTION_CANCEL = "com.vmate.downloader.ACTION_CANCEL_NOTIFICATION"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }
}