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
import com.vmate.downloader.presentation.VideoDetailActivity

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
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_enter_url),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val intent = Intent(requireContext(), VideoDetailActivity::class.java).apply {
                putExtra(VideoDetailActivity.EXTRA_URL, url)
            }
            startActivity(intent)
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