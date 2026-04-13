package com.vmate.downloader.presentation.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vmate.downloader.R
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

        // Auto-populate filename as the user types a URL
        binding.etUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val url = s?.toString()?.trim() ?: ""
                val suggested = extractFilename(url)
                if (binding.etFilename.text.isNullOrBlank() || isAutoFilename(binding.etFilename.text.toString())) {
                    binding.etFilename.setText(suggested)
                }
            }
        })

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnDownload.setOnClickListener {
            val url = binding.etUrl.text?.toString()?.trim() ?: ""
            if (!isValidUrl(url)) {
                Toast.makeText(requireContext(), getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val filename = binding.etFilename.text?.toString()?.trim()
                ?.ifBlank { extractFilename(url) }
                ?: extractFilename(url)
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

    private fun isValidUrl(url: String): Boolean {
        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) return false
        return try {
            val uri = java.net.URI(url)
            uri.host != null && uri.host.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    private fun extractFilename(url: String): String {
        if (url.isBlank()) return ""
        return try {
            val path = url.substringBefore("?").substringBefore("#")
            val name = path.substringAfterLast("/").trim()
            name.ifBlank { "download_${System.currentTimeMillis()}" }
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }

    /** Returns true when the filename looks like it was auto-generated so we can overwrite it. */
    private fun isAutoFilename(name: String): Boolean =
        name.startsWith("download_") || name.isEmpty()

    companion object {
        const val TAG = "AddDownloadBottomSheet"
    }
}