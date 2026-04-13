package com.vmate.downloader.domain.models

data class DownloadPreferences(
    val extractAudio: Boolean = false,
    val videoQuality: VideoQuality = VideoQuality.BEST,
    val videoFormat: VideoFormat = VideoFormat.MP4,
    val audioQuality: AudioQuality = AudioQuality.BEST,
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val embedMetadata: Boolean = false,
    val embedThumbnail: Boolean = false,
    val downloadThumbnail: Boolean = false,
    val playlist: Boolean = false,
    val playlistStart: Int = 1,
    val playlistEnd: Int? = null,
    val subdirectoryByUploader: Boolean = false,
    val subdirectoryByPlaylist: Boolean = false
)

enum class VideoQuality(val label: String, val height: Int?) {
    BEST("Best", null),
    P1080("1080p", 1080),
    P720("720p", 720),
    P480("480p", 480),
    P360("360p", 360),
    P240("240p", 240)
}

enum class VideoFormat(val ext: String) {
    MP4("mp4"),
    WEBM("webm"),
    MKV("mkv")
}

enum class AudioQuality(val label: String, val bitrate: Int?) {
    BEST("Best", null),
    K320("320kbps", 320),
    K256("256kbps", 256),
    K192("192kbps", 192),
    K128("128kbps", 128)
}

enum class AudioFormat(val ext: String) {
    MP3("mp3"),
    M4A("m4a"),
    WAV("wav"),
    OPUS("opus"),
    VORBIS("vorbis")
}
