package com.vmate.downloader.domain.models

data class VideoInfo(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val description: String,
    val channel: String,
    val uploaderUrl: String,
    val durationSeconds: Int,
    val viewCount: Int,
    val likeCount: Int,
    val uploadDate: String,
    val formats: List<String>,
    val isPlaylist: Boolean,
    val playlistTitle: String?,
    val playlistCount: Int?
)