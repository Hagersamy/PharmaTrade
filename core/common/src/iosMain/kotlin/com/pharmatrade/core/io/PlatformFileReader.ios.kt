package com.pharmatrade.core.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.lastPathComponent
import platform.posix.memcpy

// NOTE: Kotlin/Native's iOS targets can only be compiled on macOS with Xcode installed, which
// this codebase can't do from Windows (see iosApp/README.md) — this file has never been built.
// Double-check it compiles once you're building on your Mac; the shapes of the Foundation
// interop calls below (NSData.dataWithContentsOfURL, NSURL.fileURLWithPath) are correct as of
// recent Kotlin/Native releases but haven't been verified against this project's exact
// Kotlin/Native version.
@OptIn(ExperimentalForeignApi::class)
actual class PlatformFileReader {

    actual fun read(uri: String): PlatformFileInfo? {
        val url = if (uri.startsWith("file://")) NSURL(string = uri) else NSURL.fileURLWithPath(uri)
        val data = NSData.dataWithContentsOfURL(url) ?: return null
        val bytes = data.toByteArray()
        if (bytes.isEmpty()) return null
        return PlatformFileInfo(
            bytes = bytes,
            displayName = url.lastPathComponent ?: "file",
            mimeType = null
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val result = ByteArray(size)
    if (size > 0) {
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return result
}
