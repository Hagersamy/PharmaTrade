package com.pharmatrade.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.pharmatrade.core.common.i18n.AppLanguage
import com.pharmatrade.core.common.i18n.ArabicStrings
import com.pharmatrade.core.common.i18n.EnglishStrings
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.i18n.LocalStrings

// onPrimary/onSecondary are text/icon color drawn on top of a still-colored button in both
// themes, so they stay white rather than following AppColors.surfaceWhite (which is the
// screen's card/surface background and needs to flip to a dark tone in dark mode).
private fun colorScheme(colors: AppColors, dark: Boolean): ColorScheme = if (dark) {
    darkColorScheme(
        primary = colors.primaryBlue,
        onPrimary = Color.White,
        primaryContainer = colors.primaryBlueContainer,
        onPrimaryContainer = colors.primaryBlueLight,
        secondary = colors.secondaryGreen,
        onSecondary = Color.White,
        secondaryContainer = colors.secondaryGreenContainer,
        onSecondaryContainer = colors.secondaryGreenDark,
        tertiary = colors.warningAmber,
        tertiaryContainer = colors.warningAmberContainer,
        error = colors.errorRed,
        errorContainer = colors.errorRedContainer,
        background = colors.backgroundGray,
        onBackground = colors.textPrimary,
        surface = colors.surfaceWhite,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.cardGray,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.dividerGray
    )
} else {
    lightColorScheme(
        primary = colors.primaryBlue,
        onPrimary = Color.White,
        primaryContainer = colors.primaryBlueContainer,
        onPrimaryContainer = colors.primaryBlueDark,
        secondary = colors.secondaryGreen,
        onSecondary = Color.White,
        secondaryContainer = colors.secondaryGreenContainer,
        onSecondaryContainer = colors.secondaryGreenDark,
        tertiary = colors.warningAmber,
        tertiaryContainer = colors.warningAmberContainer,
        error = colors.errorRed,
        errorContainer = colors.errorRedContainer,
        background = colors.backgroundGray,
        onBackground = colors.textPrimary,
        surface = colors.surfaceWhite,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.cardGray,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.dividerGray
    )
}

@Composable
fun PharmaTradeTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val appColors = if (isDark) DarkAppColors else LightAppColors
    val scheme = colorScheme(appColors, isDark)
    ApplyStatusBarStyle(scheme)

    val language by LanguageManager.language.collectAsState()
    val strings = if (language == AppLanguage.ARABIC) ArabicStrings else EnglishStrings
    val direction = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalStrings provides strings,
        LocalLayoutDirection provides direction
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PharmaTypography,
            content = content
        )
    }
}

// Android tints the system status bar to match the app; other platforms don't have an
// equivalent concept for a Compose app to control, so they no-op.
@Composable
expect fun ApplyStatusBarStyle(colorScheme: ColorScheme)
