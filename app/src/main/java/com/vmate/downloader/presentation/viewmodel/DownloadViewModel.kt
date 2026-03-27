package com.vmate.downloader.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vmate.downloader.data.local.DownloadDatabase
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.service.DownloadForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = DownloadDatabase.getInstance(application).downloadDao()

    val downloads: StateFlow<List<Download>> = dao.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDownload(download: Download) = viewModelScope.launch {
        dao.deleteDownload(download)
    }

    fun cancelDownload(id: Long) {
        val context = getApplication<Application>()
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            action = DownloadForegroundService.ACTION_CANCEL
            putExtra(DownloadForegroundService.EXTRA_DOWNLOAD_ID, id)
        }
        context.startService(intent)
    }
}