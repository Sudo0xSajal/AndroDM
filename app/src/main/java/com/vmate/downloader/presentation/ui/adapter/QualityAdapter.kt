package com.vmate.downloader.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vmate.downloader.databinding.QualityItemBinding
import com.vmate.downloader.domain.models.QualityOption

class QualityAdapter(
    private val options: List<QualityOption>,
    private var selectedPosition: Int = 0,
    private val onSelectionChanged: (QualityOption) -> Unit
) : RecyclerView.Adapter<QualityAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: QualityItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(option: QualityOption, isSelected: Boolean) {
            binding.tvQuality.text = option.quality
            binding.tvFileSize.text = option.fileSize
            binding.rbQuality.isChecked = isSelected

            binding.root.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
                onSelectionChanged(option)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = QualityItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(options[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = options.size
}
