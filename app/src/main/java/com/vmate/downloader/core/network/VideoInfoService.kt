package com.vmate.downloader.core.network

import androidx.annotation.WorkerThread
import com.vmate.downloader.domain.models.FormatInfo
import com.vmate.downloader.domain.models.VideoInfo
import okhttp3.Request
import java.net.URL

object VideoInfoService {

    /** Sentinel value used when a playlist's total video count cannot be determined. */
    const val UNKNOWN_PLAYLIST_COUNT = 50


    /**
     * Fetches basic video information from a URL using HTTP headers.
     * For direct media URLs, this returns size, mime-type, and a single format entry.
     * For YouTube/platform URLs, metadata is extracted from the URL pattern
     * and populated as best-effort.
     */
    @WorkerThread
    fun fetchVideoInfo(url: String): VideoInfo? {
        return try {
            val parsed = URL(url)
            val host = parsed.host.lowercase()

            return when {
                isYouTubeUrl(host) -> buildYouTubeInfo(url)
                else -> buildDirectInfo(url)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isYouTubeUrl(host: String): Boolean =
        host.contains("youtube.com") || host.contains("youtu.be") ||
                host.contains("youtube-nocookie.com")

    private fun buildYouTubeInfo(url: String): VideoInfo {
        val videoId = extractYouTubeId(url) ?: ""
        val thumbnailUrl = if (videoId.isNotEmpty())
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null

        val isPlaylist = isYouTubePlaylist(url)
        val formats = buildDefaultVideoFormats(url)
        val audioFormats = buildDefaultAudioFormats(url)

        return VideoInfo(
            id = videoId,
            url = url,
            title = if (isPlaylist) "YouTube Playlist" else "YouTube Video",
            thumbnailUrl = thumbnailUrl,
            description = null,
            channel = null,
            uploaderUrl = null,
            durationSeconds = null,
            viewCount = null,
            likeCount = null,
            uploadDate = null,
            formats = formats + audioFormats,
            isPlaylist = isPlaylist,
            playlistTitle = if (isPlaylist) "YouTube Playlist" else null,
            // playlistCount cannot be determined without authenticated API/yt-dlp;
            // set to a non-zero sentinel so the UI can show the playlist selection dialog.
            playlistCount = if (isPlaylist) UNKNOWN_PLAYLIST_COUNT else 0
        )
    }

    private fun buildDirectInfo(url: String): VideoInfo? {
        return try {
            val request = Request.Builder().url(url).head().build()
            val response = HttpClientFactory.client.newCall(request).execute()
            val contentType = response.header("Content-Type") ?: ""
            val contentLength = response.header("Content-Length")?.toLongOrNull()
            val mimeType = contentType.split(";").first().trim()

            val filename = url.substringAfterLast("/").substringBefore("?")
                .ifBlank { "media_${System.currentTimeMillis()}" }
            val ext = filename.substringAfterLast(".").lowercase()
                .let { if (it.length in 2..4) it else "mp4" }

            val isAudio = mimeType.startsWith("audio/")
            val isVideo = mimeType.startsWith("video/")
            if (!isAudio && !isVideo) return null

            val format = FormatInfo(
                formatId = "direct",
                ext = ext,
                resolution = if (isVideo) "Auto" else null,
                fps = null,
                filesize = contentLength,
                tbr = null,
                vbr = null,
                abr = null,
                acodec = if (isAudio) ext else "aac",
                vcodec = if (isVideo) "h264" else null,
                quality = if (isVideo) "Best" else "Audio",
                isAudioOnly = isAudio,
                isVideoOnly = false,
                url = url
            )

            VideoInfo(
                id = "",
                url = url,
                title = filename,
                thumbnailUrl = null,
                description = null,
                channel = null,
                uploaderUrl = null,
                durationSeconds = null,
                viewCount = null,
                likeCount = null,
                uploadDate = null,
                formats = listOf(format)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractYouTubeId(url: String): String? {
        val patterns = listOf(
            Regex("(?:v=|/v/|youtu\\.be/|/embed/|/shorts/)([A-Za-z0-9_-]{11})"),
            Regex("^([A-Za-z0-9_-]{11})$")
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun isYouTubePlaylist(url: String): Boolean =
        url.contains("list=") || url.contains("/playlist")

    private fun buildDefaultVideoFormats(url: String): List<FormatInfo> {
        val qualities = listOf(
            Triple("1080p", 1080, "hd1080"),
            Triple("720p", 720, "hd720"),
            Triple("480p", 480, "large"),
            Triple("360p", 360, "medium"),
            Triple("240p", 240, "small")
        )
        return qualities.map { (label, _, formatId) ->
            FormatInfo(
                formatId = formatId,
                ext = "mp4",
                resolution = label,
                fps = 30,
                filesize = null,
                tbr = null,
                vbr = null,
                abr = null,
                acodec = "aac",
                vcodec = "h264",
                quality = label,
                isAudioOnly = false,
                isVideoOnly = false,
                url = url
            )
        }
    }

    private fun buildDefaultAudioFormats(url: String): List<FormatInfo> {
        val qualities = listOf(
            Triple("320kbps", 320.0, "bestaudio-320"),
            Triple("256kbps", 256.0, "bestaudio-256"),
            Triple("192kbps", 192.0, "bestaudio-192"),
            Triple("128kbps", 128.0, "bestaudio-128")
        )
        return qualities.map { (label, bitrate, formatId) ->
            FormatInfo(
                formatId = formatId,
                ext = "mp3",
                resolution = null,
                fps = null,
                filesize = null,
                tbr = bitrate,
                vbr = null,
                abr = bitrate,
                acodec = "mp3",
                vcodec = null,
                quality = label,
                isAudioOnly = true,
                isVideoOnly = false,
                url = url
            )
        }
    }
}

