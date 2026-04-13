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

    /**
     * Estimates the file size in bytes when an explicit [filesize] is unavailable.
     * Uses the total bitrate ([tbr], kbps) multiplied by [durationSeconds].
     * Returns `null` when neither [filesize] nor the required inputs are available.
     */
    fun estimatedSizeBytes(durationSeconds: Long?): Long? {
        if (filesize != null && filesize > 0) return filesize
        val kbps = tbr ?: return null
        val secs = durationSeconds ?: return null
        if (secs <= 0) return null
        // kbps × seconds / 8 → bytes
        return (kbps * 1000.0 / 8.0 * secs).toLong()
    }

    fun fileSizeLabel(durationSeconds: Long? = null): String {
        val bytes = estimatedSizeBytes(durationSeconds)
            ?: return "Unknown size"
        val estimated = filesize == null || filesize <= 0
        val label = when {
            bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
            else -> "$bytes B"
        }
        return if (estimated) "~$label" else label
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

