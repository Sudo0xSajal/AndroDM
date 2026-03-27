data class FormatInfo(
    val formatId: String,
    val ext: String,
    val resolution: String,
    val fps: Int,
    val filesize: Int,
    val tbr: Int,
    val vbr: Int,
    val abr: Int,
    val acodec: String,
    val vcodec: String,
    val quality: String,
    val isAudioOnly: Boolean,
    val isVideoOnly: Boolean,
    val url: String
)