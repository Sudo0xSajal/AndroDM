package com.vmate.downloader.core.network

import androidx.annotation.WorkerThread
import com.vmate.downloader.domain.models.FormatInfo
import com.vmate.downloader.domain.models.VideoInfo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

object VideoInfoService {

    /** Sentinel value used when a playlist's total video count cannot be determined. */
    const val UNKNOWN_PLAYLIST_COUNT = 50


    /**
     * Fetches basic video information from a URL using HTTP headers.
     * For direct media URLs, this returns size, mime-type, and a single format entry.
     * For YouTube/platform URLs, metadata is fetched via the oEmbed and InnerTube APIs.
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
        val isPlaylist = isYouTubePlaylist(url)

        // Fetch real metadata: try InnerTube first, oEmbed as fallback
        val innerTubeData = if (videoId.isNotEmpty() && !isPlaylist) {
            fetchYouTubeInnerTube(videoId)
        } else null
        val oembedData = fetchYouTubeOembed(url)

        val title = when {
            innerTubeData?.title?.isNotBlank() == true -> innerTubeData.title
            oembedData?.title?.isNotBlank() == true -> oembedData.title
            isPlaylist -> "YouTube Playlist"
            else -> "YouTube Video"
        }
        val author = innerTubeData?.author ?: oembedData?.authorName
        val thumbnailUrl = innerTubeData?.thumbnailUrl
            ?: oembedData?.thumbnailUrl
            ?: if (videoId.isNotEmpty()) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null

        // Only use formats that have real direct stream URLs from InnerTube.
        // Do NOT fall back to placeholder formats using the YouTube page URL — downloading
        // that URL would fetch the HTML page (~1 MB) instead of actual video content.
        val formats: List<FormatInfo> =
            if (innerTubeData != null && innerTubeData.formats.isNotEmpty()) {
                innerTubeData.formats.mapNotNull { innerTubeStreamToFormatInfo(it) }
            } else {
                emptyList()
            }

        return VideoInfo(
            id = videoId,
            url = url,
            title = title,
            thumbnailUrl = thumbnailUrl,
            description = null,
            channel = author,
            uploaderUrl = null,
            durationSeconds = innerTubeData?.durationSeconds,
            viewCount = innerTubeData?.viewCount,
            likeCount = null,
            uploadDate = null,
            formats = formats,
            isPlaylist = isPlaylist,
            playlistTitle = if (isPlaylist) title else null,
            // playlistCount cannot be determined without authenticated API/yt-dlp;
            // set to a non-zero sentinel so the UI can show the playlist selection dialog.
            playlistCount = if (isPlaylist) UNKNOWN_PLAYLIST_COUNT else 0,
            uploader = author
        )
    }

    // ── YouTube oEmbed API (title, author, thumbnail — no auth required) ──────

    private data class OembedData(
        val title: String,
        val authorName: String,
        val thumbnailUrl: String?
    )

    private fun fetchYouTubeOembed(url: String): OembedData? {
        return try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"
            val request = Request.Builder().url(oembedUrl).get().build()
            val response = HttpClientFactory.client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            OembedData(
                title = json.optString("title", ""),
                authorName = json.optString("author_name", ""),
                thumbnailUrl = json.optString("thumbnail_url").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            null
        }
    }

    // ── YouTube InnerTube API (stream URLs, duration, file size) ─────────────

    private data class InnerTubeStream(
        val itag: Int,
        val url: String,
        val mimeType: String,
        val quality: String,
        val qualityLabel: String?,
        val contentLength: Long?,
        val bitrate: Long?,
        val fps: Int?
    )

    private data class InnerTubeData(
        val title: String,
        val author: String,
        val durationSeconds: Long?,
        val viewCount: Long?,
        val thumbnailUrl: String?,
        val formats: List<InnerTubeStream>
    )

    private data class InnerTubeClientConfig(
        val clientName: String,
        val clientId: Int,
        val clientVersion: String,
        val androidSdkVersion: Int?,
        val userAgent: String
    )

    /**
     * Queries the YouTube InnerTube player endpoint, trying multiple client configurations
     * in order until one returns direct (non-ciphered) stream URLs.
     */
    private fun fetchYouTubeInnerTube(videoId: String): InnerTubeData? {
        val clients = listOf(
            InnerTubeClientConfig(
                clientName = "ANDROID_TESTSUITE",
                clientId = 30,
                clientVersion = "1.9",
                androidSdkVersion = 30,
                userAgent = "com.google.android.youtube/1.9 (Linux; U; Android 10) gzip"
            ),
            InnerTubeClientConfig(
                clientName = "ANDROID_CREATOR",
                clientId = 14,
                clientVersion = "24.45.100",
                androidSdkVersion = 30,
                userAgent = "com.google.android.apps.youtube.creator/24.45.100 (Linux; U; Android 10) gzip"
            ),
            InnerTubeClientConfig(
                clientName = "ANDROID",
                clientId = 3,
                clientVersion = "19.44.38",
                androidSdkVersion = 30,
                userAgent = "com.google.android.youtube/19.44.38 (Linux; U; Android 10) gzip"
            ),
            InnerTubeClientConfig(
                clientName = "IOS",
                clientId = 5,
                clientVersion = "19.45.4",
                androidSdkVersion = null,
                userAgent = "com.google.ios.youtube/19.45.4 (iPhone14,3; U; CPU iOS 16_0 like Mac OS X)"
            )
        )

        for (client in clients) {
            val result = fetchYouTubeInnerTubeWithClient(videoId, client)
            if (result != null && result.formats.isNotEmpty()) return result
        }
        return null
    }

    private fun fetchYouTubeInnerTubeWithClient(
        videoId: String,
        client: InnerTubeClientConfig
    ): InnerTubeData? {
        return try {
            val clientJson = JSONObject().apply {
                put("clientName", client.clientName)
                put("clientVersion", client.clientVersion)
                if (client.androidSdkVersion != null) {
                    put("androidSdkVersion", client.androidSdkVersion)
                    put("osName", "Android")
                    put("osVersion", "10.0")
                }
                put("hl", "en")
                put("gl", "US")
            }
            val bodyJson = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", clientJson)
                })
                put("videoId", videoId)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
            }.toString()

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", client.userAgent)
                .header("X-Youtube-Client-Name", client.clientId.toString())
                .header("X-Youtube-Client-Version", client.clientVersion)
                .header("Content-Type", "application/json")
                .build()

            val response = HttpClientFactory.client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val responseStr = response.body?.string() ?: return null
            val json = JSONObject(responseStr)

            val videoDetails = json.optJSONObject("videoDetails") ?: return null
            val streamingData = json.optJSONObject("streamingData")

            val title = videoDetails.optString("title", "")
            val author = videoDetails.optString("author", "")
            val durationSeconds = videoDetails.optString("lengthSeconds", "").toLongOrNull()
            val viewCount = videoDetails.optString("viewCount", "").toLongOrNull()

            // Use highest-resolution thumbnail available
            val thumbnailUrl = videoDetails.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?.let { thumbs ->
                    if (thumbs.length() > 0) {
                        thumbs.getJSONObject(thumbs.length() - 1)
                            .optString("url").takeIf { it.isNotBlank() }
                    } else null
                }

            val streams = mutableListOf<InnerTubeStream>()
            streamingData?.let { sd ->
                parseInnerTubeFormats(sd.optJSONArray("formats"), streams)
                parseInnerTubeFormats(sd.optJSONArray("adaptiveFormats"), streams)
            }

            InnerTubeData(
                title = title,
                author = author,
                durationSeconds = durationSeconds,
                viewCount = viewCount,
                thumbnailUrl = thumbnailUrl,
                formats = streams
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts the `url` parameter from a `signatureCipher` or `cipher` value.
     * These are URL-encoded strings of the form `s=...&sp=...&url=BASE_URL`.
     * We extract only the base URL; the download manager will attempt the
     * request and YouTube's CDN may still serve the content without enforcing
     * the signature for some streams.
     */
    private fun extractUrlFromCipher(cipher: String): String? {
        return try {
            cipher.split("&").associate { pair ->
                val idx = pair.indexOf('=')
                if (idx < 0) pair to "" else pair.substring(0, idx) to pair.substring(idx + 1)
            }["url"]?.let { URLDecoder.decode(it, "UTF-8") }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseInnerTubeFormats(
        jsonArray: JSONArray?,
        output: MutableList<InnerTubeStream>
    ) {
        jsonArray ?: return
        for (i in 0 until jsonArray.length()) {
            val f = jsonArray.getJSONObject(i)
            // Prefer a direct URL; fall back to extracting the base URL from the
            // signatureCipher/cipher field (present when YouTube encrypts stream URLs).
            val directUrl = f.optString("url", "")
            val streamUrl = if (directUrl.isNotBlank()) {
                directUrl
            } else {
                val cipher = f.optString("signatureCipher", "")
                    .ifBlank { f.optString("cipher", "") }
                if (cipher.isNotBlank()) extractUrlFromCipher(cipher) else null
            } ?: continue
            output.add(
                InnerTubeStream(
                    itag = f.optInt("itag"),
                    url = streamUrl,
                    mimeType = f.optString("mimeType", ""),
                    quality = f.optString("quality", "medium"),
                    qualityLabel = f.optString("qualityLabel").takeIf { it.isNotBlank() },
                    contentLength = f.optString("contentLength", "").toLongOrNull(),
                    bitrate = if (f.has("bitrate")) f.optLong("bitrate") else null,
                    fps = if (f.has("fps")) f.optInt("fps") else null
                )
            )
        }
    }

    private fun innerTubeStreamToFormatInfo(stream: InnerTubeStream): FormatInfo? {
        val mimeBase = stream.mimeType.split(";").first().trim()
        val isAudio = mimeBase.startsWith("audio/")
        val isVideo = mimeBase.startsWith("video/")
        if (!isAudio && !isVideo) return null

        val ext = when (mimeBase) {
            "video/mp4" -> "mp4"
            "video/webm" -> "webm"
            "audio/mp4" -> "m4a"
            "audio/webm" -> "webm"
            else -> if (isVideo) "mp4" else "m4a"
        }
        val quality = stream.qualityLabel ?: stream.quality

        return FormatInfo(
            formatId = stream.itag.toString(),
            ext = ext,
            resolution = if (isVideo) quality else null,
            fps = stream.fps,
            filesize = stream.contentLength,
            tbr = stream.bitrate?.div(1000.0),
            vbr = if (isVideo) stream.bitrate?.div(1000.0) else null,
            abr = if (isAudio) stream.bitrate?.div(1000.0) else null,
            acodec = if (isAudio) ext else "aac",
            vcodec = if (isVideo) "h264" else null,
            quality = quality,
            isAudioOnly = isAudio,
            isVideoOnly = isVideo && !isAudio,
            url = stream.url
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
}

