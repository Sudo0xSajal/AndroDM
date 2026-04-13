package com.vmate.downloader.presentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vmate.downloader.databinding.FragmentFormatSelectionBinding
import com.vmate.downloader.domain.models.FormatInfo
import com.vmate.downloader.domain.models.VideoInfo
import com.vmate.downloader.presentation.ui.adapter.FormatAdapter

class FormatSelectionFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFormatSelectionBinding? = null
    private val binding get() = _binding!!

    private var videoInfo: VideoInfo? = null
    private var onFormatSelected: ((FormatInfo) -> Unit)? = null
    private var selectedFormat: FormatInfo? = null

    private lateinit var videoAdapter: FormatAdapter
    private lateinit var audioAdapter: FormatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormatSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val info = videoInfo ?: return

        setupVideoFormats(info.videoFormats())
        setupAudioFormats(info.audioFormats())

        binding.btnConfirm.setOnClickListener {
            selectedFormat?.let { format ->
                onFormatSelected?.invoke(format)
                dismiss()
            }
        }
    }

    private fun setupVideoFormats(formats: List<FormatInfo>) {
        if (formats.isEmpty()) {
            binding.tvVideoHeader.visibility = View.GONE
            binding.rvVideoFormats.visibility = View.GONE
            return
        }
        binding.tvVideoHeader.visibility = View.VISIBLE
        binding.rvVideoFormats.visibility = View.VISIBLE

        if (selectedFormat == null) selectedFormat = formats.first()

        videoAdapter = FormatAdapter { format ->
            selectedFormat = format
            if (::audioAdapter.isInitialized) audioAdapter.clearSelection()
        }
        videoAdapter.submitList(formats)
        binding.rvVideoFormats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
        }
    }

    private fun setupAudioFormats(formats: List<FormatInfo>) {
        if (formats.isEmpty()) {
            binding.tvAudioHeader.visibility = View.GONE
            binding.rvAudioFormats.visibility = View.GONE
            return
        }
        binding.tvAudioHeader.visibility = View.VISIBLE
        binding.rvAudioFormats.visibility = View.VISIBLE

        audioAdapter = FormatAdapter { format ->
            selectedFormat = format
            if (::videoAdapter.isInitialized) videoAdapter.clearSelection()
        }
        audioAdapter.submitList(formats)
        binding.rvAudioFormats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = audioAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FormatSelectionFragment"

        fun newInstance(
            videoInfo: VideoInfo,
            onFormatSelected: (FormatInfo) -> Unit
        ): FormatSelectionFragment {
            return FormatSelectionFragment().apply {
                this.videoInfo = videoInfo
                this.onFormatSelected = onFormatSelected
            }
        }
    }
}
