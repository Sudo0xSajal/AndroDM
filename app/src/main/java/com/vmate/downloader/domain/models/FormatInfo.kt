package com.vmate.downloader.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FormatInfo(
    val formatId: String,
    val ext: String,
    val resolution: String?,
    val fps: Int?,
    val filesize: Long?,
    val tbr: Double?,
    val vbr: Double?,
    val abr: Double?,
    val acodec: String?,
    val vcodec: String?,
    val quality: String,
    val isAudioOnly: Boolean,
    val isVideoOnly: Boolean,
    val url: String
) : Parcelable {

    fun fileSizeLabel(): String {
        val bytes = filesize ?: return "Unknown size"
        return when {
            bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
            else -> "$bytes B"
        }
    }

    fun qualityLabel(): String = when {
        isAudioOnly -> buildString {
            append(quality)
            if (abr != null) append(" • %.0fkbps".format(abr))
            append(" • ${ext.uppercase()}")
        }
        else -> buildString {
            append(quality)
            if (fps != null && fps > 0) append(" ${fps}fps")
            append(" • ${ext.uppercase()}")
            if (filesize != null) append(" • ${fileSizeLabel()}")
        }
    }
}
