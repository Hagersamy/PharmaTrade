package com.pharmatrade.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

// TODO: iOS status bar style is normally set via UIViewController.preferredStatusBarStyle,
// which needs a bridge from the hosting UIViewController — not attempted yet since there's no
// iOS app shell to wire it into (see iosApp/README.md). No-op for now.
@Composable
actual fun ApplyStatusBarStyle(colorScheme: ColorScheme) {
}
