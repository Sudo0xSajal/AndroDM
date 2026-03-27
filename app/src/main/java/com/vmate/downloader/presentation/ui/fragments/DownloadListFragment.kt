package com.vmate.downloader.presentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.vmate.downloader.databinding.FragmentDownloadListBinding
import com.vmate.downloader.presentation.ui.adapter.DownloadAdapter
import com.vmate.downloader.presentation.viewmodel.DownloadViewModel
import kotlinx.coroutines.launch

class DownloadListFragment : Fragment() {

    private var _binding: FragmentDownloadListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DownloadViewModel by viewModels()
    private lateinit var adapter: DownloadAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = DownloadAdapter { download -> viewModel.deleteDownload(download) }
        binding.rvDownloads.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDownloads.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.downloads.collect { downloads ->
                    adapter.submitList(downloads)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}