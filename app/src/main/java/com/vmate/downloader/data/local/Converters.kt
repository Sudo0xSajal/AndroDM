package com.vmate.downloader.data.local

import androidx.room.TypeConverter
import com.vmate.downloader.domain.models.DownloadStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}