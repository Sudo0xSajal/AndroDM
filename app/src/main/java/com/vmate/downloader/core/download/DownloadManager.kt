package com.vmate.downloader.core.download

import android.content.Context
import com.vmate.downloader.core.network.HttpClientFactory
import com.vmate.downloader.data.local.DownloadDatabase
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.domain.models.DownloadStatus
import kotlinx.coroutines.*
import okhttp3.Request
import java.io.File

class DownloadManager(private val context: Context) {

    private val dao = DownloadDatabase.getInstance(context).downloadDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = mutableMapOf<Long, Job>()

    fun startDownload(download: Download) {
        scope.launch {
            val id = dao.insertDownload(download.copy(status = DownloadStatus.QUEUED))
            activeJobs[id] = coroutineContext[Job]!!
            try {
                val request = Request.Builder().url(download.url).build()
                val response = HttpClientFactory.client.newCall(request).execute()
                val body = response.body ?: throw Exception("Empty response body")
                val totalBytes = body.contentLength()

                dao.updateDownload(download.copy(id = id, status = DownloadStatus.DOWNLOADING))

                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                dir.mkdirs()
                val file = File(dir, download.filename)

                var downloaded = 0L
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            downloaded += bytes
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
                dao.updateDownload(
                    download.copy(
                        id = id,
                        totalBytes = totalBytes,
                        downloadedBytes = downloaded,
                        status = DownloadStatus.COMPLETED
                    )
                )
            } catch (e: CancellationException) {
                dao.updateDownload(download.copy(id = id, status = DownloadStatus.CANCELLED))
            } catch (e: Exception) {
                dao.updateDownload(download.copy(id = id, status = DownloadStatus.FAILED))
            } finally {
                activeJobs.remove(id)
            }
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