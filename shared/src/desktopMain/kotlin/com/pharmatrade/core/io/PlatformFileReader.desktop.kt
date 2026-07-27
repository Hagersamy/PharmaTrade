package com.pharmatrade.core.io

import java.io.File
import java.net.URLConnection

// `uri` is expected to be a plain absolute file path on desktop (e.g. from AWT's FileDialog
// or Compose Desktop's file picker), not a content:// or file:// URI.
actual class PlatformFileReader {

    actual fun read(uri: String): PlatformFileInfo? {
        return try {
            val file = File(uri)
            if (!file.isFile) return null
            val bytes = file.readBytes()
            if (bytes.isEmpty()) return null
            PlatformFileInfo(
                bytes = bytes,
                displayName = file.name,
                mimeType = URLConnection.guessContentTypeFromName(file.name)
            )
        } catch (e: Exception) {
            null
        }
    }
}
