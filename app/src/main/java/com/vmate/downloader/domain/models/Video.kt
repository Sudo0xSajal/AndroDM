package com.vmate.downloader.domain.models

data class Video(
    val url: String,
    val title: String,
    val mimeType: String = "video/mp4",
    val sizeBytes: Long = 0L
)