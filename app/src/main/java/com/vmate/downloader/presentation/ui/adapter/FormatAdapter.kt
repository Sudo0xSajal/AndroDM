package com.vmate.downloader.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vmate.downloader.R
import com.vmate.downloader.databinding.ItemFormatBinding
import com.vmate.downloader.domain.models.FormatInfo

class FormatAdapter(
    private val onFormatSelected: (FormatInfo) -> Unit
) : ListAdapter<FormatInfo, FormatAdapter.FormatViewHolder>(FormatDiffCallback()) {

    private var selectedPosition: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormatViewHolder {
        val binding = ItemFormatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FormatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FormatViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    inner class FormatViewHolder(private val binding: ItemFormatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(format: FormatInfo, isSelected: Boolean) {
            binding.tvQuality.text = format.quality
            binding.tvDetails.text = format.qualityLabel()
            binding.tvFileSize.text = format.fileSizeLabel()

            val context = binding.root.context
            if (isSelected) {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.selected_format_bg)
                )
                binding.tvQuality.setTextColor(
                    ContextCompat.getColor(context, R.color.selected_format_text)
                )
                binding.ivSelected.visibility = android.view.View.VISIBLE
            } else {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.unselected_format_bg)
                )
                binding.tvQuality.setTextColor(
                    ContextCompat.getColor(context, R.color.on_surface_secondary)
                )
                binding.ivSelected.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                val adapterPos = bindingAdapterPosition
                if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener
                val previousPosition = selectedPosition
                selectedPosition = adapterPos
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onFormatSelected(format)
            }
        }
    }

    fun setSelectedPosition(position: Int) {
        val previous = selectedPosition
        selectedPosition = position
        notifyItemChanged(previous)
        if (position >= 0) notifyItemChanged(selectedPosition)
    }

    fun clearSelection() {
        val previous = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (previous >= 0) notifyItemChanged(previous)
    }

    private class FormatDiffCallback : DiffUtil.ItemCallback<FormatInfo>() {
        override fun areItemsTheSame(oldItem: FormatInfo, newItem: FormatInfo) =
            oldItem.formatId == newItem.formatId

        override fun areContentsTheSame(oldItem: FormatInfo, newItem: FormatInfo) =
            oldItem == newItem
    }
}
