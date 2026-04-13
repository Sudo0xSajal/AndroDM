package com.vmate.downloader.core.download

import android.content.Context
import com.vmate.downloader.core.network.HttpClientFactory
import com.vmate.downloader.data.local.DownloadDatabase
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.domain.models.DownloadStatus
import kotlinx.coroutines.*
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DownloadManager(private val context: Context) {

    private val dao = DownloadDatabase.getInstance(context).downloadDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Long, Job>()

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
                            // Exponential backoff before retry: 2s, 4s, 8s (after 1st, 2nd, 3rd failure)
                            delay(RETRY_DELAY_MS * (1L shl (attempt - 1)))
                        }
                    }
                }
                if (lastException != null) {
                    dao.updateDownload(download.copy(id = id, status = DownloadStatus.FAILED))
                }
            } catch (e: CancellationException) {
                dao.updateDownload(download.copy(id = id, status = DownloadStatus.CANCELLED))
            } finally {
                activeJobs.remove(id)
            }
        }
    }

    private suspend fun executeDownload(id: Long, download: Download) {
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        dir.mkdirs()
        val file = File(dir, download.filename)
        // Remove any partial file from a previous attempt before writing.
        // Ignore the return value: if deletion fails we still attempt the write,
        // which will overwrite the stale content anyway.
        if (file.exists()) file.delete()

        val request = Request.Builder().url(download.url).build()
        HttpClientFactory.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${response.message}")
            val body = response.body ?: throw Exception("Empty response body")
            // contentLength() returns -1 when the server does not send Content-Length
            val totalBytes = body.contentLength()

            dao.updateDownload(download.copy(id = id, status = DownloadStatus.DOWNLOADING))

            var downloaded = 0L
            var lastReportedProgress = -1
            var lastReportedBytes = 0L

            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes

                        // Throttle DB writes: update only when progress % changes (when total
                        // size is known) or when at least 512 KB more has been downloaded
                        // (when total size is unknown) to avoid flooding the database.
                        val currentProgress = if (totalBytes > 0)
                            ((downloaded * 100) / totalBytes).toInt()
                        else
                            -1

                        val shouldReport = when {
                            totalBytes > 0 -> currentProgress != lastReportedProgress
                            else           -> (downloaded - lastReportedBytes) >= 512 * 1024
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
                        }
                    }
                }
            }

            dao.updateDownload(
                download.copy(
                    id = id,
                    totalBytes = if (totalBytes > 0) totalBytes else downloaded,
                    downloadedBytes = downloaded,
                    status = DownloadStatus.COMPLETED
                )
            )
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