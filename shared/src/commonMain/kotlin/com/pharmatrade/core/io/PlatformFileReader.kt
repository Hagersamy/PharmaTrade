package com.pharmatrade.core.io

// Generic result of reading whatever a platform file picker returned — raw bytes plus
// whatever metadata the platform can resolve. Callers apply their own filename/mime-type
// override policy on top of this (e.g. forcing an image mime type for a licence photo, or
// sniffing a spreadsheet mime type from the extension), since that's app-specific business
// logic rather than platform I/O.
data class PlatformFileInfo(
    val bytes: ByteArray,
    val displayName: String,
    val mimeType: String?
)

// `uri` is whatever platform-native reference a file picker handed back: an Android
// content:// Uri string, an iOS file path/URL, or a Desktop absolute file path.
expect class PlatformFileReader {
    fun read(uri: String): PlatformFileInfo?
}
