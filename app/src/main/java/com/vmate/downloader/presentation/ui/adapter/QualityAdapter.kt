package com.vmate.downloader.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vmate.downloader.R
import com.vmate.downloader.databinding.ItemQualityBinding
import com.vmate.downloader.domain.models.VideoQuality

class QualityAdapter(
    private val items: List<VideoQuality>,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<QualityAdapter.ViewHolder>() {

    /** Index of the selected item, or [NO_SELECTION] when nothing is selected. */
    var selectedPosition: Int = if (items.isNotEmpty()) 0 else NO_SELECTION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQualityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = items.size

    fun clearSelection() {
        val previous = selectedPosition
        selectedPosition = NO_SELECTION
        if (previous >= 0 && previous < itemCount) {
            notifyItemChanged(previous)
        }
    }

    inner class ViewHolder(private val binding: ItemQualityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(quality: VideoQuality, isSelected: Boolean) {
            binding.tvQuality.text = quality.quality
            binding.tvFileSize.text = quality.fileSize
            binding.rbQuality.isChecked = isSelected

            val strokeColor = ContextCompat.getColor(
                binding.root.context,
                if (isSelected) R.color.quality_selected_stroke else R.color.quality_unselected_stroke
            )
            binding.cardQuality.strokeColor = strokeColor
            binding.cardQuality.strokeWidth =
                binding.root.context.resources.getDimensionPixelSize(
                    if (isSelected) R.dimen.quality_stroke_selected else R.dimen.quality_stroke_default
                )

            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val previous = selectedPosition
                selectedPosition = pos
                if (previous >= 0 && previous < itemCount) {
                    notifyItemChanged(previous)
                }
                notifyItemChanged(selectedPosition)
                onSelectionChanged(selectedPosition)
            }
        }
    }

    companion object {
        /** Sentinel value indicating that no item is selected. */
        const val NO_SELECTION = -1
    }
}
