package com.vmate.downloader.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QualityOption(
    val quality: String,
    val fileSize: String,
    val url: String
) : Parcelable
