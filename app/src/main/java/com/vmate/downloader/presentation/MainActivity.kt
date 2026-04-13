package com.vmate.downloader.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vmate.downloader.databinding.ActivityMainBinding
import com.vmate.downloader.domain.models.VideoQuality
import com.vmate.downloader.presentation.ui.fragments.AddDownloadBottomSheet
import com.vmate.downloader.presentation.ui.fragments.DownloadListFragment
import com.vmate.downloader.presentation.ui.fragments.QualitySelectionBottomSheet

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, DownloadListFragment())
                .commit()
        }

        binding.fabAdd.setOnClickListener {
            requestNotificationPermissionIfNeeded()
            AddDownloadBottomSheet().show(supportFragmentManager, AddDownloadBottomSheet.TAG)
        }

        binding.fabQuality.setOnClickListener {
            showQualitySelectionDemo()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showQualitySelectionDemo() {
        val audioQuality = VideoQuality(
            quality = "Audio Only",
            fileSize = "6.8 MB",
            url = "https://example.com/audio.mp3"
        )
        val videoQualities = listOf(
            VideoQuality("1080p (Full HD)", "120 MB", "https://example.com/1080p.mp4"),
            VideoQuality("720p",            "80 MB",  "https://example.com/720p.mp4"),
            VideoQuality("480p",            "45 MB",  "https://example.com/480p.mp4"),
            VideoQuality("360p",            "25 MB",  "https://example.com/360p.mp4")
        )
        QualitySelectionBottomSheet.newInstance(audioQuality, videoQualities) { selected ->
            Toast.makeText(
                this,
                "Downloading: ${selected.quality} (${selected.fileSize})",
                Toast.LENGTH_SHORT
            ).show()
        }.show(supportFragmentManager, QualitySelectionBottomSheet.TAG)
    }
}
