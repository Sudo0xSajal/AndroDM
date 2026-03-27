package com.vmate.downloader.presentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vmate.downloader.R
import com.vmate.downloader.databinding.BottomSheetQualitySelectionBinding
import com.vmate.downloader.domain.models.VideoQuality
import com.vmate.downloader.presentation.ui.adapter.QualityAdapter

/**
 * A reusable [BottomSheetDialogFragment] that lets the user choose a download quality.
 *
 * Example usage:
 * ```kotlin
 * val audioQuality = VideoQuality("Audio Only", "6.8 MB", "https://example.com/audio.mp3")
 * val videoQualities = listOf(
 *     VideoQuality("1080p (Full HD)", "120 MB", "https://example.com/1080p.mp4"),
 *     VideoQuality("720p",            "80 MB",  "https://example.com/720p.mp4"),
 *     VideoQuality("480p",            "45 MB",  "https://example.com/480p.mp4"),
 *     VideoQuality("360p",            "25 MB",  "https://example.com/360p.mp4"),
 * )
 * QualitySelectionBottomSheet.newInstance(audioQuality, videoQualities) { selected ->
 *     // Handle the selected VideoQuality here
 *     startDownload(selected)
 * }.show(supportFragmentManager, QualitySelectionBottomSheet.TAG)
 * ```
 */
class QualitySelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetQualitySelectionBinding? = null
    private val binding get() = _binding!!

    private var audioQuality: VideoQuality? = null
    private var videoQualities: List<VideoQuality> = emptyList()
    private var onDownloadClicked: ((VideoQuality) -> Unit)? = null

    private lateinit var qualityAdapter: QualityAdapter

    /** True when the "Audio Only" card is the currently selected option. */
    private var isAudioSelected = false

    fun setQualities(audio: VideoQuality?, videos: List<VideoQuality>) {
        audioQuality = audio
        videoQualities = videos
    }

    fun setOnDownloadClickListener(listener: (VideoQuality) -> Unit) {
        onDownloadClicked = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetQualitySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAudioSection()
        setupVideoSection()
        setupDownloadButton()
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun setupAudioSection() {
        val audio = audioQuality
        if (audio == null) {
            binding.tvAudiosLabel.visibility = View.GONE
            binding.cardAudio.visibility = View.GONE
            return
        }
        binding.tvAudioSize.text = audio.fileSize
        binding.cardAudio.setOnClickListener {
            isAudioSelected = true
            binding.rbAudio.isChecked = true
            qualityAdapter.clearSelection()
            setAudioCardStroke(selected = true)
        }
    }

    private fun setupVideoSection() {
        qualityAdapter = QualityAdapter(videoQualities) { _ ->
            // A video option was selected – clear audio selection
            isAudioSelected = false
            binding.rbAudio.isChecked = false
            setAudioCardStroke(selected = false)
        }
        binding.rvQualities.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = qualityAdapter
        }
    }

    private fun setupDownloadButton() {
        binding.btnDownload.setOnClickListener {
            val selected: VideoQuality? = when {
                isAudioSelected -> audioQuality
                qualityAdapter.selectedPosition >= 0 ->
                    videoQualities.getOrNull(qualityAdapter.selectedPosition)
                else -> null
            }
            selected?.let {
                onDownloadClicked?.invoke(it)
                dismiss()
            }
        }
    }

    private fun setAudioCardStroke(selected: Boolean) {
        val strokeColor = ContextCompat.getColor(
            requireContext(),
            if (selected) R.color.quality_selected_stroke else R.color.quality_unselected_stroke
        )
        binding.cardAudio.strokeColor = strokeColor
        binding.cardAudio.strokeWidth = resources.getDimensionPixelSize(
            if (selected) R.dimen.quality_stroke_selected else R.dimen.quality_stroke_default
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "QualitySelectionBottomSheet"

        fun newInstance(
            audio: VideoQuality?,
            videos: List<VideoQuality>,
            onDownload: (VideoQuality) -> Unit
        ): QualitySelectionBottomSheet = QualitySelectionBottomSheet().also { sheet ->
            sheet.setQualities(audio, videos)
            sheet.setOnDownloadClickListener(onDownload)
        }
    }
}
