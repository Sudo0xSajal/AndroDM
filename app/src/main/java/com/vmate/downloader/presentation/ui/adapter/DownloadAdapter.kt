package com.vmate.downloader.presentation.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vmate.downloader.R
import com.vmate.downloader.databinding.ItemDownloadBinding
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.domain.models.DownloadStatus

class DownloadAdapter(
    private val onCancel: (Download) -> Unit,
    private val onDelete: (Download) -> Unit
) : ListAdapter<Download, DownloadAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(download: Download) {
            binding.tvFilename.text = download.filename
            binding.progressBar.progress = download.progress

            // Status chip text and colour
            val (statusLabel, statusColor) = statusLabelAndColor(download.status)
            binding.tvStatus.text = statusLabel
            val chipDrawable = binding.tvStatus.background?.mutate()
            chipDrawable?.setTint(ContextCompat.getColor(binding.root.context, statusColor))
            binding.tvStatus.background = chipDrawable

            // Progress tint to match status
            binding.progressBar.progressTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, statusColor)
            )

            // Size info
            binding.tvSpeed.text = buildSizeLabel(download)

            // Show cancel button only while active
            val isActive = download.status == DownloadStatus.DOWNLOADING ||
                    download.status == DownloadStatus.QUEUED
            binding.btnAction.visibility = if (isActive) View.VISIBLE else View.GONE
            binding.btnAction.setOnClickListener { onCancel(download) }

            // Long-press to delete
            binding.root.setOnLongClickListener {
                onDelete(download)
                true
            }
        }

        private fun statusLabelAndColor(status: DownloadStatus): Pair<String, Int> = when (status) {
            DownloadStatus.QUEUED      -> "QUEUED"      to R.color.status_queued
            DownloadStatus.DOWNLOADING -> "DOWNLOADING" to R.color.status_downloading
            DownloadStatus.PAUSED      -> "PAUSED"      to R.color.status_paused
            DownloadStatus.COMPLETED   -> "COMPLETED"   to R.color.status_completed
            DownloadStatus.FAILED      -> "FAILED"      to R.color.status_failed
            DownloadStatus.CANCELLED   -> "CANCELLED"   to R.color.status_cancelled
        }

        private fun buildSizeLabel(download: Download): String {
            val downloaded = formatBytes(download.downloadedBytes)
            return when {
                download.totalBytes > 0 ->
                    "$downloaded / ${formatBytes(download.totalBytes)}"
                download.downloadedBytes > 0 ->
                    downloaded
                else -> ""
            }
        }

        private fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1 -> "%.2f GB".format(gb)
                mb >= 1 -> "%.1f MB".format(mb)
                kb >= 1 -> "%.0f KB".format(kb)
                else    -> "$bytes B"
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Download>() {
            override fun areItemsTheSame(oldItem: Download, newItem: Download) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Download, newItem: Download) =
                oldItem == newItem
        }
    }
}