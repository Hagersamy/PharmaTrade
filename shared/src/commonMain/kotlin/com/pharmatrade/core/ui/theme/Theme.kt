package com.pharmatrade.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PharmaLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondaryGreen,
    onSecondary = SurfaceWhite,
    secondaryContainer = SecondaryGreenContainer,
    onSecondaryContainer = SecondaryGreenDark,
    tertiary = WarningAmber,
    tertiaryContainer = WarningAmberContainer,
    error = ErrorRed,
    errorContainer = ErrorRedContainer,
    background = BackgroundGray,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = CardGray,
    onSurfaceVariant = TextSecondary,
    outline = DividerGray
)

@Composable
fun PharmaTradeTheme(content: @Composable () -> Unit) {
    val colorScheme = PharmaLightColorScheme
    ApplyStatusBarStyle(colorScheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PharmaTypography,
        content = content
    )
}

// Android tints the system status bar to match the app; other platforms don't have an
// equivalent concept for a Compose app to control, so they no-op.
@Composable
expect fun ApplyStatusBarStyle(colorScheme: ColorScheme)
