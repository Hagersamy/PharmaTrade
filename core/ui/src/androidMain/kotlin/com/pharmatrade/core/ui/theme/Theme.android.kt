package com.pharmatrade.core.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun ApplyStatusBarStyle(colorScheme: ColorScheme) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        // No status bar color is set — the system's transparent edge-to-edge bar is kept, showing
        // the screen's own top bar behind it. Only the icons are styled: light, since what sits
        // behind them is the blue (or dark-mode) top bar / login background.
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
}
