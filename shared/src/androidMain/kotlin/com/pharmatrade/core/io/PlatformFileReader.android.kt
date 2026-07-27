package com.pharmatrade.core.io

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

actual class PlatformFileReader(private val context: Context) {

    actual fun read(uri: String): PlatformFileInfo? {
        return try {
            val parsed = Uri.parse(uri)
            val stream = context.contentResolver.openInputStream(parsed) ?: return null
            val bytes = stream.use { it.readBytes() }
            if (bytes.isEmpty()) return null
            PlatformFileInfo(
                bytes = bytes,
                displayName = resolveDisplayName(parsed),
                mimeType = context.contentResolver.getType(parsed)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col != -1 && cursor.moveToFirst()) return cursor.getString(col) ?: ""
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: ""
    }
}
