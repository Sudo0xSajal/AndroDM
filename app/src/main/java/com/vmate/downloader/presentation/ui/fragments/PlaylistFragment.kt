package com.vmate.downloader.presentation.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vmate.downloader.R
import com.vmate.downloader.databinding.FragmentPlaylistBinding
import com.vmate.downloader.domain.models.VideoInfo
import com.vmate.downloader.presentation.ui.adapter.PlaylistVideoAdapter

class PlaylistFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private var videoInfo: VideoInfo? = null
    private var onSelectionConfirmed: ((Set<Int>) -> Unit)? = null
    private var adapter: PlaylistVideoAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val info = videoInfo ?: return

        binding.tvPlaylistTitle.text = info.playlistTitle
            ?: getString(R.string.playlist)
        binding.tvPlaylistCount.text =
            getString(R.string.playlist_video_count, info.playlistCount)

        // Build placeholder VideoInfo entries for the playlist items.
        // Actual per-video metadata (titles, durations) requires yt-dlp; here we create
        // numbered placeholders so users can select a range before starting the download.
        val playlistVideos = List(info.playlistCount) { index ->
            VideoInfo(
                id = "${index + 1}",
                url = info.url,
                title = getString(R.string.playlist_video_n, index + 1),
                thumbnailUrl = null,
                description = null,
                channel = info.channel,
                uploaderUrl = null,
                durationSeconds = null,
                viewCount = null,
                likeCount = null,
                uploadDate = null
            )
        }

        adapter = PlaylistVideoAdapter(playlistVideos) { selectedIndices ->
            binding.tvSelectedCount.text =
                getString(R.string.playlist_selected_count, selectedIndices.size)
            binding.btnConfirm.isEnabled = selectedIndices.isNotEmpty()
        }

        binding.rvPlaylistVideos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@PlaylistFragment.adapter
        }

        binding.btnSelectAll.setOnClickListener { adapter?.selectAll() }
        binding.btnClearSelection.setOnClickListener { adapter?.clearSelection() }

        binding.btnApplyRange.setOnClickListener {
            val startText = binding.etRangeStart.text?.toString()?.trim()
            val endText = binding.etRangeEnd.text?.toString()?.trim()
            val start = startText?.toIntOrNull()?.minus(1) ?: 0
            val end = endText?.toIntOrNull()?.minus(1) ?: (playlistVideos.size - 1)
            adapter?.selectRange(start, end)
        }

        binding.btnConfirm.setOnClickListener {
            val selected = adapter?.getSelectedIndices() ?: emptySet()
            onSelectionConfirmed?.invoke(selected)
            dismiss()
        }

        setupRangeInputWatchers(playlistVideos.size)
    }

    private fun setupRangeInputWatchers(total: Int) {
        binding.etRangeEnd.hint = total.toString()
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val startVal = binding.etRangeStart.text?.toString()?.toIntOrNull() ?: 1
                val endVal = binding.etRangeEnd.text?.toString()?.toIntOrNull() ?: total
                binding.btnApplyRange.isEnabled = startVal in 1..total && endVal in startVal..total
            }
        }
        binding.etRangeStart.addTextChangedListener(watcher)
        binding.etRangeEnd.addTextChangedListener(watcher)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PlaylistFragment"

        fun newInstance(
            videoInfo: VideoInfo,
            onSelectionConfirmed: (Set<Int>) -> Unit
        ): PlaylistFragment {
            return PlaylistFragment().apply {
                this.videoInfo = videoInfo
                this.onSelectionConfirmed = onSelectionConfirmed
            }
        }
    }
}
