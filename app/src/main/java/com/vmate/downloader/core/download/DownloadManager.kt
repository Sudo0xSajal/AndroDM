package com.vmate.downloader.core.download

import android.content.Context
import android.util.Log
import com.vmate.downloader.data.local.DownloadDao
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.domain.models.DownloadStatus
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

class DownloadManager(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeDownloads = mutableMapOf<Long, DownloadJob>()
    private val downloadDir = File(context.getExternalFilesDir(null), "Downloads")

    init {
        downloadDir.mkdirs()
    }

    suspend fun startDownload(download: Download): Download {
        val updatedDownload = download.copy(
            status = DownloadStatus.DOWNLOADING,
            filePath = File(downloadDir, download.fileName).absolutePath
        )
        downloadDao.updateDownload(updatedDownload)

        val downloadJob = DownloadJob(updatedDownload)
        activeDownloads[download.id] = downloadJob

        scope.launch {
            performDownload(updatedDownload, downloadJob)
        }

        return updatedDownload
    }

    suspend fun pauseDownload(downloadId: Long) {
        activeDownloads[downloadId]?.cancel()
        val download = downloadDao.getDownloadById(downloadId) ?: return
        downloadDao.updateDownloadStatus(downloadId, DownloadStatus.PAUSED)
    }

    suspend fun resumeDownload(downloadId: Long) {
        val download = downloadDao.getDownloadById(downloadId) ?: return
        startDownload(download)
    }

    suspend fun cancelDownload(downloadId: Long) {
        activeDownloads[downloadId]?.cancel()
        val download = downloadDao.getDownloadById(downloadId) ?: return
        downloadDao.updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)
        activeDownloads.remove(downloadId)

        try {
            File(download.filePath).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun performDownload(download: Download, downloadJob: DownloadJob) {
        try {
            val request = Request.Builder()
                .url(download.videoUrl)
                .header("Range", "bytes=${download.downloadedSize}-")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                handleDownloadError(download, "HTTP ${response.code}: ${response.message}")
                return
            }

            val contentLength = response.contentLength()
            val totalFileSize = download.fileSize.takeIf { it > 0 } ?: contentLength

            val file = File(download.filePath)
            val randomAccessFile = RandomAccessFile(file, "rw")
            randomAccessFile.seek(download.downloadedSize)

            val inputStream = response.body?.byteStream() ?: run {
                handleDownloadError(download, "Empty response body")
                return
            }

            var downloadedThisSession = 0L
            var lastProgressUpdate = System.currentTimeMillis()
            var lastSpeedCalculation = System.currentTimeMillis()
            var lastDownloadedSize = download.downloadedSize

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!downloadJob.isActive) {
                    randomAccessFile.close()
                    inputStream.close()
                    return
                }

                randomAccessFile.write(buffer, 0, bytesRead)
                downloadedThisSession += bytesRead
                val totalDownloaded = download.downloadedSize + downloadedThisSession

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL) {
                    val speed = calculateSpeed(
                        totalDownloaded - lastDownloadedSize,
                        currentTime - lastSpeedCalculation
                    )
                    val eta = calculateETA(
                        totalFileSize - totalDownloaded,
                        speed
                    )
                    val progress = ((totalDownloaded * 100) / totalFileSize).toInt()

                    downloadDao.updateDownloadProgress(
                        download.id,
                        DownloadStatus.DOWNLOADING,
                        progress,
                        totalDownloaded,
                        speed,
                        eta
                    )

                    lastProgressUpdate = currentTime
                    lastDownloadedSize = totalDownloaded
                    lastSpeedCalculation = currentTime
                }
            }

            randomAccessFile.close()
            inputStream.close()

            downloadDao.updateDownloadStatus(
                download.id,
                DownloadStatus.COMPLETED,
                System.currentTimeMillis()
            )
            activeDownloads.remove(download.id)

            Log.d("DownloadManager", "Download completed: ${download.fileName}")

        } catch (e: Exception) {
            Log.e("DownloadManager", "Download failed: ${e.message}", e)
            handleDownloadError(download, e.message ?: "Unknown error")
        }
    }

    private suspend fun handleDownloadError(download: Download, error: String) {
        val newRetryCount = download.retryCount + 1
        val status = if (newRetryCount < download.maxRetries) {
            DownloadStatus.PENDING
        } else {
            DownloadStatus.FAILED
        }

        downloadDao.updateDownloadError(download.id, error, newRetryCount)
        downloadDao.updateDownloadStatus(download.id, status)
        activeDownloads.remove(download.id)
    }

    private fun calculateSpeed(bytesDownloaded: Long, timeTaken: Long): Long {
        return if (timeTaken > 0) (bytesDownloaded * 1000) / timeTaken else 0
    }

    private fun calculateETA(remainingBytes: Long, speed: Long): Long {
        return if (speed > 0) remainingBytes / speed else 0
    }

    fun shutdown() {
        scope.cancel()
    }

    private data class DownloadJob(
        val download: Download,
        private val job: Job = Job()
    ) : Job by job

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_UPDATE_INTERVAL = 500L
    }
}