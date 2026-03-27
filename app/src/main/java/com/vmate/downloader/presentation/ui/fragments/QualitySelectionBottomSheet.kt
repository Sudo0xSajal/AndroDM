package com.vmate.downloader.presentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vmate.downloader.databinding.BottomSheetQualityBinding
import com.vmate.downloader.domain.models.QualityOption
import com.vmate.downloader.presentation.ui.adapter.QualityAdapter

class QualitySelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetQualityBinding? = null
    private val binding get() = _binding!!

    private var audioOptions: List<QualityOption> = emptyList()
    private var videoOptions: List<QualityOption> = emptyList()
    private var onDownloadSelected: ((QualityOption) -> Unit)? = null

    private var selectedAudioOption: QualityOption? = null
    private var selectedVideoOption: QualityOption? = null
    private var lastSelectedSource: SelectionSource = SelectionSource.NONE

    private enum class SelectionSource { NONE, AUDIO, VIDEO }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetQualityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAudioRecyclerView()
        setupVideoRecyclerView()

        if (audioOptions.isEmpty() && videoOptions.isEmpty()) {
            binding.btnDownload.isEnabled = false
        }

        binding.btnDownload.setOnClickListener {
            val selected = when (lastSelectedSource) {
                SelectionSource.AUDIO -> selectedAudioOption
                SelectionSource.VIDEO -> selectedVideoOption
                SelectionSource.NONE -> null
            }
            if (selected != null) {
                onDownloadSelected?.invoke(selected)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please select a quality option", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAudioRecyclerView() {
        if (audioOptions.isEmpty()) {
            binding.tvAudioHeader.visibility = View.GONE
            binding.rvAudio.visibility = View.GONE
            return
        }
        selectedAudioOption = audioOptions.first()
        lastSelectedSource = SelectionSource.AUDIO
        val adapter = QualityAdapter(audioOptions, 0) { selected ->
            selectedAudioOption = selected
            lastSelectedSource = SelectionSource.AUDIO
        }
        binding.rvAudio.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }

    private fun setupVideoRecyclerView() {
        if (videoOptions.isEmpty()) {
            binding.tvVideoHeader.visibility = View.GONE
            binding.rvVideo.visibility = View.GONE
            return
        }
        selectedVideoOption = videoOptions.first()
        lastSelectedSource = SelectionSource.VIDEO
        val adapter = QualityAdapter(videoOptions, 0) { selected ->
            selectedVideoOption = selected
            lastSelectedSource = SelectionSource.VIDEO
        }
        binding.rvVideo.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "QualitySelectionBottomSheet"

        fun newInstance(
            audioOptions: List<QualityOption>,
            videoOptions: List<QualityOption>,
            onDownloadSelected: (QualityOption) -> Unit
        ): QualitySelectionBottomSheet {
            return QualitySelectionBottomSheet().apply {
                this.audioOptions = audioOptions
                this.videoOptions = videoOptions
                this.onDownloadSelected = onDownloadSelected
            }
        }
    }
}
