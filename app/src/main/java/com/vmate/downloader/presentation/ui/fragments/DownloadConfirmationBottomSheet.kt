package com.vmate.downloader.presentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vmate.downloader.R
import com.vmate.downloader.databinding.BottomSheetDownloadConfirmBinding

/**
 * Confirmation sheet displayed before a download begins.
 * Shows the video title, selected quality, and estimated file size.
 */
class DownloadConfirmationBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDownloadConfirmBinding? = null
    private val binding get() = _binding!!

    private var videoTitle: String = ""
    private var quality: String = ""
    private var fileSize: String = ""
    private var onConfirmed: (() -> Unit)? = null

    fun setData(title: String, quality: String, fileSize: String) {
        videoTitle = title
        this.quality = quality
        this.fileSize = fileSize
    }

    fun setOnConfirmListener(listener: () -> Unit) {
        onConfirmed = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDownloadConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvConfirmVideoTitle.text = videoTitle
        binding.tvConfirmQuality.text = quality
        binding.tvConfirmSize.text = fileSize.ifBlank { getString(R.string.size_unknown) }

        binding.btnConfirmCancel.setOnClickListener { dismiss() }
        binding.btnConfirmDownload.setOnClickListener {
            onConfirmed?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DownloadConfirmationBottomSheet"

        fun newInstance(
            title: String,
            quality: String,
            fileSize: String,
            onConfirm: () -> Unit
        ): DownloadConfirmationBottomSheet = DownloadConfirmationBottomSheet().also { sheet ->
            sheet.setData(title, quality, fileSize)
            sheet.setOnConfirmListener(onConfirm)
        }
    }
}
