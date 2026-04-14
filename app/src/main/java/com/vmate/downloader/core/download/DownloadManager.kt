package com.vmate.downloader.core.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.vmate.downloader.core.network.HttpClientFactory
import com.vmate.downloader.data.local.DownloadDatabase
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.domain.models.DownloadStatus
import kotlinx.coroutines.*
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class DownloadManager(private val context: Context) {

    private val dao = DownloadDatabase.getInstance(context).downloadDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    /** Optional callback so the foreground service can update its notification. */
    var progressCallback: ProgressCallback? = null

    interface ProgressCallback {
        fun onProgress(id: Long, filename: String, progress: Int, downloadedBytes: Long, totalBytes: Long)
        fun onComplete(id: Long, filename: String)
        fun onFailed(id: Long, filename: String, error: String)
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2_000L
    }

    fun startDownload(download: Download) {
        scope.launch {
            val id = dao.insertDownload(download.copy(status = DownloadStatus.QUEUED))
            activeJobs[id] = coroutineContext[Job]!!
            try {
                var attempt = 0
                var lastException: Exception? = null
                while (attempt <= MAX_RETRIES) {
                    try {
                        executeDownload(id, download)
                        lastException = null
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastException = e
                        attempt++
                        if (attempt <= MAX_RETRIES) {
                            // Exponential backoff before retry: 2s, 4s, 8s
                            delay(RETRY_DELAY_MS * (1L shl (attempt - 1)))
                        }
                    }
                }
                if (lastException != null) {
                    dao.updateDownload(download.copy(id = id, status = DownloadStatus.FAILED))
                    progressCallback?.onFailed(id, download.filename, lastException.message ?: "Unknown error")
                }
            } catch (e: CancellationException) {
                dao.updateDownload(download.copy(id = id, status = DownloadStatus.CANCELLED))
            } finally {
                activeJobs.remove(id)
            }
        }
    }

    private suspend fun executeDownload(id: Long, download: Download) {
        val request = Request.Builder().url(download.url).build()
        HttpClientFactory.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${response.message}")
            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()

            dao.updateDownload(download.copy(id = id, status = DownloadStatus.DOWNLOADING))
            progressCallback?.onProgress(id, download.filename, 0, 0L, totalBytes)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeViaMediaStore(id, download, body, totalBytes)
            } else {
                writeToPublicDownloads(id, download, body, totalBytes)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun writeViaMediaStore(
        id: Long,
        download: Download,
        body: ResponseBody,
        totalBytes: Long
    ) {
        val mimeType = getMimeType(download.filename)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, download.filename)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
        ) ?: throw Exception("Failed to create MediaStore entry for: ${download.filename}")

        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw Exception("Failed to open OutputStream for MediaStore URI")
            val downloaded = streamBodyToOutput(id, download, body, totalBytes, outputStream)

            // Mark the entry as complete so it is visible to the Files app
            val update = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            context.contentResolver.update(uri, update, null, null)

            dao.updateDownload(
                download.copy(
                    id = id,
                    totalBytes = if (totalBytes > 0) totalBytes else downloaded,
                    downloadedBytes = downloaded,
                    status = DownloadStatus.COMPLETED
                )
            )
            progressCallback?.onComplete(id, download.filename)
        } catch (e: Exception) {
            // Remove the incomplete pending entry on failure
            context.contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private suspend fun writeToPublicDownloads(
        id: Long,
        download: Download,
        body: ResponseBody,
        totalBytes: Long
    ) {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        val file = File(dir, download.filename)
        if (file.exists()) file.delete()

        val downloaded = streamBodyToOutput(id, download, body, totalBytes, file.outputStream())

        dao.updateDownload(
            download.copy(
                id = id,
                totalBytes = if (totalBytes > 0) totalBytes else downloaded,
                downloadedBytes = downloaded,
                status = DownloadStatus.COMPLETED
            )
        )
        progressCallback?.onComplete(id, download.filename)
    }

    /**
     * Streams [body] bytes into [output], reporting progress through the DAO and [progressCallback].
     * Returns the total number of bytes written.
     */
    private suspend fun streamBodyToOutput(
        id: Long,
        download: Download,
        body: ResponseBody,
        totalBytes: Long,
        output: OutputStream
    ): Long {
        var downloaded = 0L
        var lastReportedProgress = -1
        var lastReportedBytes = 0L

        body.byteStream().use { input ->
            output.use { out ->
                val buffer = ByteArray(8 * 1024)
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    out.write(buffer, 0, bytes)
                    downloaded += bytes

                    val currentProgress = if (totalBytes > 0)
                        ((downloaded * 100) / totalBytes).toInt()
                    else -1

                    val shouldReport = when {
                        totalBytes > 0 -> currentProgress != lastReportedProgress
                        else -> (downloaded - lastReportedBytes) >= 512 * 1024
                    }

                    if (shouldReport) {
                        lastReportedProgress = currentProgress
                        lastReportedBytes = downloaded
                        dao.updateDownload(
                            download.copy(
                                id = id,
                                totalBytes = totalBytes,
                                downloadedBytes = downloaded,
                                status = DownloadStatus.DOWNLOADING
                            )
                        )
                        if (currentProgress >= 0) {
                            progressCallback?.onProgress(
                                id, download.filename, currentProgress, downloaded, totalBytes
                            )
                        }
                    }
                }
            }
        }
        return downloaded
    }

    private fun getMimeType(filename: String): String {
        return when (filename.substringAfterLast('.', "").lowercase()) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "aac" -> "audio/aac"
            else -> "application/octet-stream"
        }
    }

    fun cancelDownload(id: Long) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
    }

    fun cancelAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }
}