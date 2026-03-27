package com.vmate.downloader.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vmate.downloader.databinding.ActivityMainBinding
import com.vmate.downloader.presentation.ui.fragments.AddDownloadBottomSheet
import com.vmate.downloader.presentation.ui.fragments.DownloadListFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, DownloadListFragment())
                .commit()
        }

        binding.fabAdd.setOnClickListener {
            AddDownloadBottomSheet().show(supportFragmentManager, AddDownloadBottomSheet.TAG)
        }
    }
}