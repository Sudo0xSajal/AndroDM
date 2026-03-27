package com.vmate.downloader.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vmate.downloader.core.network.VideoInfoService
import com.vmate.downloader.data.local.DownloadDatabase
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.domain.models.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = DownloadDatabase.getInstance(application).downloadDao()

    val downloads: StateFlow<List<Download>> = dao.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _videoInfoState = MutableStateFlow<VideoInfoState>(VideoInfoState.Idle)
    val videoInfoState: StateFlow<VideoInfoState> = _videoInfoState.asStateFlow()

    fun deleteDownload(download: Download) = viewModelScope.launch {
        dao.deleteDownload(download)
    }

    fun fetchVideoInfo(url: String) {
        if (_videoInfoState.value is VideoInfoState.Loading) return
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