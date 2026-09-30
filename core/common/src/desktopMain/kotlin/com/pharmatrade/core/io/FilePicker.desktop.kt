package com.pharmatrade.core.io

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberFilePickerLauncher(
    mimeTypes: List<String>,
    onResult: (uri: String, displayName: String) -> Unit
): () -> Unit {
    return remember(mimeTypes) {
        {
            val chooser = JFileChooser()
            val extensions = mimeTypes.mapNotNull(::mimeToExtension)
            if (extensions.isNotEmpty()) {
                chooser.fileFilter = FileNameExtensionFilter(extensions.joinToString(", "), *extensions.toTypedArray())
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                onResult(chooser.selectedFile.absolutePath, chooser.selectedFile.name)
            }
        }
    }
}

private fun mimeToExtension(mime: String): String? = when (mime) {
    "application/vnd.ms-excel" -> "xls"
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
    "text/csv", "text/comma-separated-values" -> "csv"
    else -> null
}
