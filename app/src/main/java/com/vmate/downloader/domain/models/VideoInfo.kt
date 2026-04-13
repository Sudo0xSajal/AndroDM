package com.vmate.downloader.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoInfo(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val description: String?,
    val channel: String?,
    val uploaderUrl: String?,
    val durationSeconds: Long?,
    val viewCount: Long?,
    val likeCount: Long?,
    val uploadDate: String?,
    val formats: List<FormatInfo> = emptyList(),
    val isPlaylist: Boolean = false,
    val playlistTitle: String? = null,
    val playlistCount: Int = 0,
    val playlistIndex: Int? = null,
    val uploader: String? = null
) : Parcelable {

    fun durationLabel(): String {
        val secs = durationSeconds ?: return ""
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun videoFormats(): List<FormatInfo> = formats.filter { !it.isAudioOnly }

    fun audioFormats(): List<FormatInfo> = formats.filter { it.isAudioOnly }
}
