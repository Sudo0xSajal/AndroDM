package com.vmate.downloader.data.repository

import android.content.Context
import com.vmate.downloader.data.local.DownloadDatabase
import com.vmate.downloader.domain.models.Download
import kotlinx.coroutines.flow.Flow

class DownloadRepository(context: Context) {

    private val dao = DownloadDatabase.getInstance(context).downloadDao()

    fun getAllDownloads(): Flow<List<Download>> = dao.getAllDownloads()

    suspend fun insertDownload(download: Download): Long = dao.insertDownload(download)

    suspend fun updateDownload(download: Download) = dao.updateDownload(download)

    suspend fun deleteDownload(download: Download) = dao.deleteDownload(download)

    suspend fun getDownloadById(id: Long): Download? = dao.getDownloadById(id)
}
