package com.vmate.downloader.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vmate.downloader.databinding.ItemDownloadBinding
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.domain.models.DownloadStatus

class DownloadAdapter(private val onDelete: (Download) -> Unit) :
    ListAdapter<Download, DownloadAdapter.ViewHolder>(DIFF_CALLBACK) {

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
            binding.tvStatus.text = download.status.name
            binding.tvSpeed.text = when (download.status) {
                DownloadStatus.COMPLETED -> "100%"
                DownloadStatus.DOWNLOADING -> "${download.progress}%"
                else -> ""
            }
            binding.root.setOnLongClickListener {
                onDelete(download)
                true
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