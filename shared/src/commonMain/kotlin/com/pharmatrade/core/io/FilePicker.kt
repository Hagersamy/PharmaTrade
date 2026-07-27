package com.pharmatrade.core.io

import androidx.compose.runtime.Composable

// Returns a launch function that opens the platform's native file picker restricted to
// `mimeTypes`. The picked file's platform-native uri (Android content:// Uri, iOS file URL,
// Desktop absolute path) is delivered to `onResult`, ready to hand to PlatformFileReader.read(),
// along with a best-effort display name (e.g. "inventory.xlsx") for immediate UI feedback —
// the authoritative name still comes from PlatformFileReader.read() at actual-read time.
@Composable
expect fun rememberFilePickerLauncher(
    mimeTypes: List<String>,
    onResult: (uri: String, displayName: String) -> Unit
): () -> Unit
