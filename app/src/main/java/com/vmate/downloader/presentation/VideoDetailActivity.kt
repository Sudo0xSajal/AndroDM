package com.vmate.downloader.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.snackbar.Snackbar
import com.vmate.downloader.R
import com.vmate.downloader.databinding.ActivityVideoDetailBinding
import com.vmate.downloader.domain.models.Download
import com.vmate.downloader.domain.models.FormatInfo
import com.vmate.downloader.domain.models.VideoInfo
import com.vmate.downloader.domain.models.VideoQuality
import com.vmate.downloader.presentation.ui.fragments.DownloadConfirmationBottomSheet
import com.vmate.downloader.presentation.ui.fragments.FormatSelectionFragment
import com.vmate.downloader.presentation.ui.fragments.PlaylistFragment
import com.vmate.downloader.presentation.ui.fragments.QualitySelectionBottomSheet
import com.vmate.downloader.presentation.viewmodel.DownloadViewModel
import com.vmate.downloader.service.DownloadForegroundService
import com.vmate.downloader.util.ConnectivityChecker
import com.vmate.downloader.util.StorageChecker
import kotlinx.coroutines.launch

class VideoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoDetailBinding
    private val viewModel: DownloadViewModel by viewModels()

    private var selectedFormat: FormatInfo? = null
    private var currentVideoInfo: VideoInfo? = null
    private var currentUrl: String = ""

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
        currentUrl = url

        // Always set up the state observer, regardless of connectivity,
        // so the retry button can trigger a fetch that updates the UI.
        observeVideoInfo()

        if (!ConnectivityChecker.isConnected(this)) {
            showError(getString(R.string.error_no_internet))
        } else {
            viewModel.fetchVideoInfo(url)
        }

        binding.btnRetry.setOnClickListener {
            if (!ConnectivityChecker.isConnected(this)) {
                showError(getString(R.string.error_no_internet))
                return@setOnClickListener
            }
            viewModel.fetchVideoInfo(url)
        }

        binding.btnCopyUrl.setOnClickListener { view ->
            copyUrlToClipboard(view)
        }

        binding.btnDownload.setOnClickListener {
            showQualitySelection()
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
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.videoInfoState.collect { state ->
                    when (state) {
                        is DownloadViewModel.VideoInfoState.Idle -> {
                            // No-op: initial state before a fetch is triggered.
                            // The UI is already set up by the connectivity check in onCreate.
                        }
                        is DownloadViewModel.VideoInfoState.Loading -> showLoading(true)
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
            binding.tvPlaylistSelection.text = getString(R.string.playlist_all_selected)
        } else {
            binding.playlistGroup.visibility = View.GONE
        }

        val allFormats = info.formats
        if (allFormats.isNotEmpty()) {
            selectedFormat = allFormats.firstOrNull { !it.isAudioOnly } ?: allFormats.first()
            updateSelectedFormatDisplay(selectedFormat!!)
            binding.btnSelectFormat.visibility = View.VISIBLE
            binding.btnDownload.isEnabled = true
        } else {
            binding.btnSelectFormat.visibility = View.GONE
            binding.tvSelectedFormat.text = getString(R.string.error_no_format)
            binding.tvSelectedFormat.visibility = View.VISIBLE
            binding.btnDownload.isEnabled = false
        }
    }

    private fun updateSelectedFormatDisplay(format: FormatInfo) {
        binding.tvSelectedFormat.text = format.qualityLabel()
        binding.tvSelectedFormat.visibility = View.VISIBLE
    }

    // ─── Copy-to-clipboard ───────────────────────────────────────────────────

    private fun copyUrlToClipboard(view: View) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("video_url", currentUrl))

        // Haptic feedback
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        // Swap icon to checkmark temporarily
        binding.btnCopyUrl.setIconResource(R.drawable.ic_check)
        binding.btnCopyUrl.iconTint = ContextCompat.getColorStateList(this, R.color.copy_success)

        Snackbar.make(binding.root, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show()

        // Reset icon after 2 s; guard against view being destroyed before the delay fires.
        binding.btnCopyUrl.postDelayed({
            if (!isDestroyed && !isFinishing) {
                binding.btnCopyUrl.setIconResource(R.drawable.ic_copy)
                binding.btnCopyUrl.iconTint = null
            }
        }, 2_000L)
    }

    // ─── Quality selection → confirmation → download ─────────────────────────

    private fun showQualitySelection() {
        val info = currentVideoInfo ?: return
        val duration = info.durationSeconds

        val audioFormat = info.audioFormats().firstOrNull()
        val videoFormats = info.videoFormats()

        if (videoFormats.isEmpty() && audioFormat == null) {
            Toast.makeText(this, getString(R.string.error_no_format), Toast.LENGTH_SHORT).show()
            return
        }

        val audioQuality = audioFormat?.let {
            VideoQuality(
                quality = it.qualityLabel(),
                fileSize = it.fileSizeLabel(duration),
                url = it.url
            )
        }

        val videoQualities = videoFormats.map { fmt ->
            VideoQuality(
                quality = fmt.quality,
                fileSize = fmt.fileSizeLabel(duration),
                url = fmt.url
            )
        }

        QualitySelectionBottomSheet.newInstance(audioQuality, videoQualities) { selected ->
            showDownloadConfirmation(info, selected)
        }.show(supportFragmentManager, QualitySelectionBottomSheet.TAG)
    }

    private fun showDownloadConfirmation(info: VideoInfo, quality: VideoQuality) {
        DownloadConfirmationBottomSheet.newInstance(
            title = info.title,
            quality = quality.quality,
            fileSize = quality.fileSize
        ) {
            executeDownload(info, quality)
        }.show(supportFragmentManager, DownloadConfirmationBottomSheet.TAG)
    }

    private fun executeDownload(info: VideoInfo, quality: VideoQuality) {
        if (!ConnectivityChecker.isConnected(this)) {
            Toast.makeText(this, getString(R.string.error_no_internet), Toast.LENGTH_LONG).show()
            return
        }

        // Estimate required bytes for storage check (best-effort)
        val requiredBytes = info.formats
            .firstOrNull { it.url == quality.url }
            ?.estimatedSizeBytes(info.durationSeconds) ?: 0L

        if (!StorageChecker.hasEnoughSpace(requiredBytes)) {
            Toast.makeText(this, getString(R.string.error_insufficient_storage), Toast.LENGTH_LONG)
                .show()
            return
        }

        // Prefer the FormatInfo matching this quality URL, fall back to selectedFormat
        val format = info.formats.firstOrNull { it.url == quality.url } ?: selectedFormat
        val downloadUrl = format?.url ?: quality.url
        val filename = buildFilename(info, format)

        val download = Download(url = downloadUrl, filename = filename)
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

    // ─── UI state helpers ─────────────────────────────────────────────────────

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
        // Only reset ViewModel state when the activity is truly finishing (e.g., back navigation),
        // not on configuration changes (rotation) where the ViewModel should be retained.
        if (isFinishing) viewModel.resetVideoInfo()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val TAG = "VideoDetailActivity"
    }
}

