package com.vmate.downloader.util

import android.os.Environment
import android.os.StatFs

object StorageChecker {

    /**
     * Returns the number of free bytes on external storage.
     * Falls back to internal storage if external is unavailable.
     */
    fun availableBytes(): Long {
        val path = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            Environment.getExternalStorageDirectory()
        } else {
            Environment.getDataDirectory()
        }
        return try {
            val stat = StatFs(path.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }

    /** Returns `true` when at least [requiredBytes] are available. */
    fun hasEnoughSpace(requiredBytes: Long): Boolean =
        requiredBytes <= 0L || availableBytes() >= requiredBytes
}
