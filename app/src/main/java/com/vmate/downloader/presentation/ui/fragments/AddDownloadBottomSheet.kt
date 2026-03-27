package com.vmate.downloader.presentation.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vmate.downloader.databinding.BottomSheetAddDownloadBinding
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.service.DownloadForegroundService

class AddDownloadBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddDownloadBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnDownload.setOnClickListener {
            val url = binding.etUrl.text?.toString()?.trim() ?: ""
            if (url.isBlank()) {
                Toast.makeText(requireContext(), "Please enter a URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val filename = url.substringAfterLast("/").substringBefore("?")
                .ifBlank { "download_${System.currentTimeMillis()}" }
            val download = Download(url = url, filename = filename)
            val intent = Intent(requireContext(), DownloadForegroundService::class.java).apply {
                action = DownloadForegroundService.ACTION_START
                putExtra(DownloadForegroundService.EXTRA_DOWNLOAD, download)
            }
            requireContext().startService(intent)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddDownloadBottomSheet"
    }
}