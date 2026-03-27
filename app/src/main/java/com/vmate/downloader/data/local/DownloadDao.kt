package com.vmate.downloader.data.local

import androidx.room.*
import com.vmate.downloader.domain.models.Download
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<Download>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: Download): Long

    @Update
    suspend fun updateDownload(download: Download)

    @Delete
    suspend fun deleteDownload(download: Download)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): Download?
}