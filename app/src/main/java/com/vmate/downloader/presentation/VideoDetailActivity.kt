package com.vmate.downloader.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.RoundedCornersTransformation
import com.vmate.downloader.R
import com.vmate.downloader.databinding.ActivityVideoDetailBinding
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.domain.models.FormatInfo
import com.vmate.downloader.domain.models.VideoInfo
import com.vmate.downloader.presentation.ui.fragments.FormatSelectionFragment
import com.vmate.downloader.presentation.ui.fragments.PlaylistFragment
import com.vmate.downloader.presentation.viewmodel.DownloadViewModel
import com.vmate.downloader.service.DownloadForegroundService
import kotlinx.coroutines.launch

class VideoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoDetailBinding
    private val viewModel: DownloadViewModel by viewModels()

    private var selectedFormat: FormatInfo? = null
    private var currentVideoInfo: VideoInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.video_detail_title)

        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.error_invalid_url), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        observeVideoInfo()
        viewModel.fetchVideoInfo(url)

        binding.btnRetry.setOnClickListener {
            viewModel.fetchVideoInfo(url)
        }

        binding.btnDownload.setOnClickListener {
            startDownload()
        }

        binding.btnSelectFormat.setOnClickListener {
            currentVideoInfo?.let { info ->
                FormatSelectionFragment.newInstance(info) { format ->
                    selectedFormat = format
                    updateSelectedFormatDisplay(format)
                }.show(supportFragmentManager, FormatSelectionFragment.TAG)
            }
        }

        binding.btnPlaylist.setOnClickListener {
            currentVideoInfo?.let { info ->
                if (info.isPlaylist) {
                    PlaylistFragment.newInstance(info) { selectedIndices ->
                        binding.tvPlaylistSelection.text =
                            getString(R.string.playlist_selected_count, selectedIndices.size)
                    }.show(supportFragmentManager, PlaylistFragment.TAG)
                }
            }
        }
    }

    private fun observeVideoInfo() {
        lifecycleScope.launch {
            viewModel.videoInfoState.collect { state ->
                when (state) {
                    is DownloadViewModel.VideoInfoState.Idle -> {
                        showLoading(true)
                    }
                    is DownloadViewModel.VideoInfoState.Loading -> {
                        showLoading(true)
                    }
                    is DownloadViewModel.VideoInfoState.Success -> {
                        showLoading(false)
                        displayVideoInfo(state.info)
                    }
                    is DownloadViewModel.VideoInfoState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun displayVideoInfo(info: VideoInfo) {
        currentVideoInfo = info
        binding.errorGroup.visibility = View.GONE
        binding.contentGroup.visibility = View.VISIBLE

        binding.tvTitle.text = info.title
        binding.tvChannel.text = info.channel ?: info.uploader ?: ""
        binding.tvDuration.text = if (info.durationSeconds != null)
            getString(R.string.duration_label, info.durationLabel())
        else ""

        if (info.thumbnailUrl != null) {
            binding.ivThumbnail.load(info.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_video_placeholder)
                error(R.drawable.ic_video_placeholder)
                transformations(RoundedCornersTransformation(8f))
            }
            binding.ivThumbnail.visibility = View.VISIBLE
        } else {
            binding.ivThumbnail.setImageResource(R.drawable.ic_video_placeholder)
            binding.ivThumbnail.visibility = View.VISIBLE
        }

        if (info.isPlaylist) {
            binding.playlistGroup.visibility = View.VISIBLE
            binding.tvPlaylistTitle.text = info.playlistTitle ?: getString(R.string.playlist)
            binding.tvPlaylistCount.text =
                getString(R.string.playlist_video_count, info.playlistCount)
            binding.tvPlaylistSelection.text =
                getString(R.string.playlist_all_selected)
        } else {
            binding.playlistGroup.visibility = View.GONE
        }

        val allFormats = info.formats
        if (allFormats.isNotEmpty()) {
            selectedFormat = allFormats.first()
            updateSelectedFormatDisplay(allFormats.first())
            binding.btnSelectFormat.visibility = View.VISIBLE
        } else {
            binding.btnSelectFormat.visibility = View.GONE
        }

        binding.btnDownload.isEnabled = true
    }

    private fun updateSelectedFormatDisplay(format: FormatInfo) {
        binding.tvSelectedFormat.text = format.qualityLabel()
        binding.tvSelectedFormat.visibility = View.VISIBLE
    }

    private fun startDownload() {
        val info = currentVideoInfo ?: return
        val format = selectedFormat

        val downloadUrl = format?.url ?: info.url
        val filename = buildFilename(info, format)

        val download = Download(
            url = downloadUrl,
            filename = filename
        )

        val intent = Intent(this, DownloadForegroundService::class.java).apply {
            action = DownloadForegroundService.ACTION_START
            putExtra(DownloadForegroundService.EXTRA_DOWNLOAD, download)
        }
        startService(intent)
        Toast.makeText(this, getString(R.string.download_started), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun buildFilename(info: VideoInfo, format: FormatInfo?): String {
        val safeName = info.title
            .replace(Regex("[^a-zA-Z0-9_\\-. ]"), "_")
            .trim()
            .take(100)
            .ifBlank { "download_${System.currentTimeMillis()}" }
        val ext = format?.ext ?: "mp4"
        return "$safeName.$ext"
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.contentGroup.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.errorGroup.visibility = View.GONE
        binding.btnDownload.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.contentGroup.visibility = View.GONE
        binding.errorGroup.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        viewModel.resetVideoInfo()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val TAG = "VideoDetailActivity"
    }
}
