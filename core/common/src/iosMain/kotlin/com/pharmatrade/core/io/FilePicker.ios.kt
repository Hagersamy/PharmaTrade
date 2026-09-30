package com.pharmatrade.core.io

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject

// NOTE: Kotlin/Native's iOS targets can only be compiled on macOS with Xcode installed, which
// this codebase can't do from Windows (see iosApp/README.md) — this file has never been built.
// Double-check the UIDocumentPickerViewController/delegate interop shapes below once you're
// building on your Mac; exact generated Kotlin signatures for UIKit APIs can shift slightly
// between Kotlin/Native versions. Also note: files picked from outside the app sandbox are
// "security-scoped" — reading them may need url.startAccessingSecurityScopedResource() before
// PlatformFileReader.read() and stopAccessingSecurityScopedResource() after, which isn't wired
// up yet (PlatformFileReader.ios.kt predates this file and doesn't do it either).
@Composable
actual fun rememberFilePickerLauncher(
    mimeTypes: List<String>,
    onResult: (uri: String, displayName: String) -> Unit
): () -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                val uriString = url.absoluteString ?: return
                currentOnResult.value(uriString, url.lastPathComponent ?: "")
            }
        }
    }
    return remember(mimeTypes, delegate) {
        {
            val documentTypes = mimeTypes.map(::mimeToUti).ifEmpty { listOf("public.data") }
            val picker = UIDocumentPickerViewController(
                documentTypes = documentTypes,
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
            )
            picker.delegate = delegate
            UIApplication.sharedApplication.keyWindow?.rootViewController
                ?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private fun mimeToUti(mime: String): String = when (mime) {
    "text/csv", "text/comma-separated-values" -> "public.comma-separated-values-text"
    "application/vnd.ms-excel" -> "com.microsoft.excel.xls"
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "org.openxmlformats.spreadsheetml.sheet"
    else -> "public.data"
}
