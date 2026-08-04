package com.pharmatrade.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Every screen in the app references the semantic colors below directly (e.g. `color = TextPrimary`,
// `.background(BackgroundGray)`) rather than through MaterialTheme.colorScheme. To support dark mode
// without touching every one of those call sites, each name below is a @Composable property that
// resolves through this CompositionLocal instead of a fixed constant — PharmaTradeTheme provides the
// light or dark AppColors instance for the whole tree, and every existing usage picks it up for free
// since it's already inside a @Composable.
data class AppColors(
    val primaryBlue: Color,
    val primaryBlueDark: Color,
    val primaryBlueLight: Color,
    val primaryBlueContainer: Color,
    val secondaryGreen: Color,
    val secondaryGreenDark: Color,
    val secondaryGreenLight: Color,
    val secondaryGreenContainer: Color,
    val warningAmber: Color,
    val warningAmberContainer: Color,
    val errorRed: Color,
    val errorRedContainer: Color,
    val surfaceWhite: Color,
    val backgroundGray: Color,
    val cardGray: Color,
    val dividerGray: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    val discountBadge: Color,
    val discountBadgeContainer: Color
)

val LightAppColors = AppColors(
    primaryBlue = Color(0xFF1E40AF),
    primaryBlueDark = Color(0xFF1E3A8A),
    primaryBlueLight = Color(0xFF3B82F6),
    primaryBlueContainer = Color(0xFFDBEAFE),
    secondaryGreen = Color(0xFF059669),
    secondaryGreenDark = Color(0xFF047857),
    secondaryGreenLight = Color(0xFF10B981),
    secondaryGreenContainer = Color(0xFFD1FAE5),
    warningAmber = Color(0xFFD97706),
    warningAmberContainer = Color(0xFFFEF3C7),
    errorRed = Color(0xFFDC2626),
    errorRedContainer = Color(0xFFFEE2E2),
    surfaceWhite = Color(0xFFFFFFFF),
    backgroundGray = Color(0xFFF8FAFC),
    cardGray = Color(0xFFF1F5F9),
    dividerGray = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    textHint = Color(0xFF94A3B8),
    discountBadge = Color(0xFF7C3AED),
    discountBadgeContainer = Color(0xFFEDE9FE)
)

val DarkAppColors = AppColors(
    // Accent hues are lifted a step or two lighter than their light-mode counterparts so they stay
    // legible against dark surfaces (e.g. secondaryGreenDark is used throughout as body text color,
    // not just a background tint, so it needs real contrast against a dark card, not a dark tone).
    primaryBlue = Color(0xFF2563EB),
    primaryBlueDark = Color(0xFF1E3A8A),
    primaryBlueLight = Color(0xFF93C5FD),
    primaryBlueContainer = Color(0xFF1E3A8A),
    secondaryGreen = Color(0xFF10B981),
    secondaryGreenDark = Color(0xFF34D399),
    secondaryGreenLight = Color(0xFF6EE7B7),
    secondaryGreenContainer = Color(0xFF065F46),
    warningAmber = Color(0xFFFBBF24),
    warningAmberContainer = Color(0xFF78350F),
    // Material's own recommended dark-theme error color — desaturated on purpose, since a fully
    // saturated red (fine on a white background) reads as neon/alarming against a dark surface.
    errorRed = Color(0xFFCF6679),
    errorRedContainer = Color(0xFF7F1D1D),
    surfaceWhite = Color(0xFF1E293B),
    backgroundGray = Color(0xFF0F172A),
    cardGray = Color(0xFF334155),
    dividerGray = Color(0xFF334155),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    textHint = Color(0xFF64748B),
    // Desaturated (not a vivid violet) for the same reason as errorRed above.
    discountBadge = Color(0xFF9575CD),
    discountBadgeContainer = Color(0xFF4C1D95)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val PrimaryBlue: Color @Composable get() = LocalAppColors.current.primaryBlue
val PrimaryBlueDark: Color @Composable get() = LocalAppColors.current.primaryBlueDark
val PrimaryBlueLight: Color @Composable get() = LocalAppColors.current.primaryBlueLight
val PrimaryBlueContainer: Color @Composable get() = LocalAppColors.current.primaryBlueContainer

val SecondaryGreen: Color @Composable get() = LocalAppColors.current.secondaryGreen
val SecondaryGreenDark: Color @Composable get() = LocalAppColors.current.secondaryGreenDark
val SecondaryGreenLight: Color @Composable get() = LocalAppColors.current.secondaryGreenLight
val SecondaryGreenContainer: Color @Composable get() = LocalAppColors.current.secondaryGreenContainer

val WarningAmber: Color @Composable get() = LocalAppColors.current.warningAmber
val WarningAmberContainer: Color @Composable get() = LocalAppColors.current.warningAmberContainer

val ErrorRed: Color @Composable get() = LocalAppColors.current.errorRed
val ErrorRedContainer: Color @Composable get() = LocalAppColors.current.errorRedContainer

val SurfaceWhite: Color @Composable get() = LocalAppColors.current.surfaceWhite
val BackgroundGray: Color @Composable get() = LocalAppColors.current.backgroundGray
val CardGray: Color @Composable get() = LocalAppColors.current.cardGray
val DividerGray: Color @Composable get() = LocalAppColors.current.dividerGray
val TextPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalAppColors.current.textSecondary
val TextHint: Color @Composable get() = LocalAppColors.current.textHint

val DiscountBadge: Color @Composable get() = LocalAppColors.current.discountBadge
val DiscountBadgeContainer: Color @Composable get() = LocalAppColors.current.discountBadgeContainer
