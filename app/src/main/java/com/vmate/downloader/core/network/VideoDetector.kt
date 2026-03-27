package com.vmate.downloader.core.network

import androidx.annotation.WorkerThread
import com.vmate.downloader.domain.models.Video
import okhttp3.Request

object VideoDetector {
    private val videoMimeTypes = setOf(
        "video/mp4", "video/webm", "video/x-matroska", "video/quicktime",
        "video/x-msvideo", "video/mpeg", "video/3gpp"
    )

    @WorkerThread
    fun detectVideo(url: String): Video? {
        return try {
            val request = Request.Builder().url(url).head().build()
            val response = HttpClientFactory.client.newCall(request).execute()
            val contentType = response.header("Content-Type") ?: ""
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
            val mimeType = contentType.split(";").first().trim()
            if (mimeType in videoMimeTypes) {
                val filename = url.substringAfterLast("/").substringBefore("?")
                    .ifBlank { "video_${System.currentTimeMillis()}.mp4" }
                Video(url = url, title = filename, mimeType = mimeType, sizeBytes = contentLength)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}