package com.vmate.downloader.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vmate.downloader.core.network.VideoInfoService
import com.vmate.downloader.data.repository.DownloadRepository
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.domain.models.VideoInfo
import com.vmate.downloader.service.DownloadForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)

    val downloads: StateFlow<List<Download>> = repository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _videoInfoState = MutableStateFlow<VideoInfoState>(VideoInfoState.Idle)
    val videoInfoState: StateFlow<VideoInfoState> = _videoInfoState.asStateFlow()

    fun deleteDownload(download: Download) = viewModelScope.launch {
        repository.deleteDownload(download)
    }

    fun cancelDownload(id: Long) {
        val context = getApplication<Application>()
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            action = DownloadForegroundService.ACTION_CANCEL
            putExtra(DownloadForegroundService.EXTRA_DOWNLOAD_ID, id)
        }
        context.startService(intent)
    }

    fun fetchVideoInfo(url: String) {
        val current = _videoInfoState.value
        // Do not start a new fetch if one is already in-flight or data is already loaded.
        if (current is VideoInfoState.Loading || current is VideoInfoState.Success) return
        _videoInfoState.value = VideoInfoState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val info = VideoInfoService.fetchVideoInfo(url)
            _videoInfoState.value = if (info != null) {
                VideoInfoState.Success(info)
            } else {
                VideoInfoState.Error("Could not fetch video info. Please check the URL.")
            }
        }
    }

    fun resetVideoInfo() {
        _videoInfoState.value = VideoInfoState.Idle
    }

    sealed interface VideoInfoState {
        data object Idle : VideoInfoState
        data object Loading : VideoInfoState
        data class Success(val info: VideoInfo) : VideoInfoState
        data class Error(val message: String) : VideoInfoState
    }
}
