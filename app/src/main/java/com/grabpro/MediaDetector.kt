package com.grabpro

/**
 * Central media classifier shared by browser sniffing and future extraction routes.
 *
 * A URL extension wins when it is recognized. Resource hints are used for extensionless
 * URLs and may be either a file extension or a MIME type.
 */
data class MediaClassification(
    val extension: String,
    val category: String,
    val isManifest: Boolean = false
)

object MediaDetector {
    private val categories = mapOf(
        "Video" to setOf("mp4", "webm", "mkv", "mov", "avi", "m3u8", "m4v", "flv", "3gp", "ogv", "ts", "mpd"),
        "Audio" to setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus"),
        "Images" to setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif", "heic", "tiff"),
        "Documents" to setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "epub", "txt", "rtf", "odt", "ods", "odp", "csv"),
        "Archives/Apps" to setOf("zip", "rar", "7z", "gz", "tar", "iso", "apk", "exe", "dmg")
    )

    private val mimeExtensions = mapOf(
        "application/vnd.apple.mpegurl" to "m3u8",
        "application/x-mpegurl" to "m3u8",
        "audio/mpegurl" to "m3u8",
        "application/dash+xml" to "mpd",
        "video/mp2t" to "ts",
        "video/mp4" to "mp4",
        "video/webm" to "webm",
        "audio/mpeg" to "mp3",
        "audio/mp4" to "m4a",
        "audio/aac" to "aac",
        "audio/wav" to "wav",
        "audio/flac" to "flac",
        "audio/ogg" to "ogg",
        "image/jpeg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
        "image/gif" to "gif",
        "application/pdf" to "pdf"
    )

    private val allExtensions = categories.values.flatten().toSet()

    fun classify(url: String, hint: String? = null): MediaClassification? {
        if (!url.startsWith("http", ignoreCase = true)) return null
        val path = url.substringBefore('?').substringBefore('#')
        val urlExt = path.substringAfterLast('/', "").substringAfterLast('.', "").lowercase()
        val hintValue = hint?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        val hintedExt = mimeExtensions[hintValue]
            ?: hintValue.removePrefix(".").takeIf { it in allExtensions }
        val ext = urlExt.takeIf { it in allExtensions } ?: hintedExt ?: return null
        val category = categories.entries.firstOrNull { ext in it.value }?.key ?: return null
        return MediaClassification(ext, category, ext == "m3u8" || ext == "mpd")
    }
}
