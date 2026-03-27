package com.vmate.downloader.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vmate.downloader.databinding.ItemPlaylistVideoBinding
import com.vmate.downloader.domain.models.VideoInfo

class PlaylistVideoAdapter(
    private val videos: List<VideoInfo>,
    private val onSelectionChanged: (Set<Int>) -> Unit
) : RecyclerView.Adapter<PlaylistVideoAdapter.PlaylistVideoViewHolder>() {

    private val selectedIndices = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistVideoViewHolder {
        val binding = ItemPlaylistVideoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlaylistVideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistVideoViewHolder, position: Int) {
        holder.bind(videos[position], position, position in selectedIndices)
    }

    override fun getItemCount(): Int = videos.size

    fun selectAll() {
        selectedIndices.addAll(videos.indices)
        notifyDataSetChanged()
        onSelectionChanged(selectedIndices.toSet())
    }

    fun clearSelection() {
        selectedIndices.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedIndices.toSet())
    }

    fun selectRange(start: Int, end: Int) {
        selectedIndices.clear()
        val realEnd = minOf(end, videos.size - 1)
        val realStart = maxOf(start, 0)
        for (i in realStart..realEnd) selectedIndices.add(i)
        notifyDataSetChanged()
        onSelectionChanged(selectedIndices.toSet())
    }

    fun getSelectedIndices(): Set<Int> = selectedIndices.toSet()

    inner class PlaylistVideoViewHolder(private val binding: ItemPlaylistVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(video: VideoInfo, position: Int, isSelected: Boolean) {
            binding.tvIndex.text = "${position + 1}"
            binding.tvTitle.text = video.title
            binding.tvChannel.text = video.channel ?: video.uploader ?: ""
            binding.tvDuration.text = video.durationLabel()
            binding.checkbox.isChecked = isSelected

            binding.root.setOnClickListener {
                val adapterPos = bindingAdapterPosition
                if (adapterPos in selectedIndices) {
                    selectedIndices.remove(adapterPos)
                } else {
                    selectedIndices.add(adapterPos)
                }
                notifyItemChanged(adapterPos)
                onSelectionChanged(selectedIndices.toSet())
            }

            binding.checkbox.setOnClickListener {
                val adapterPos = bindingAdapterPosition
                if (adapterPos in selectedIndices) {
                    selectedIndices.remove(adapterPos)
                } else {
                    selectedIndices.add(adapterPos)
                }
                notifyItemChanged(adapterPos)
                onSelectionChanged(selectedIndices.toSet())
            }
        }
    }
}
