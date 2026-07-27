package com.pharmatrade.core.io

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFilePickerLauncher(
    mimeTypes: List<String>,
    onResult: (uri: String, displayName: String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (col != -1 && cursor.moveToFirst()) cursor.getString(col) else null
            } ?: uri.lastPathSegment?.substringAfterLast('/') ?: ""
            onResult(uri.toString(), name)
        }
    }
    return { launcher.launch(mimeTypes.toTypedArray()) }
}
