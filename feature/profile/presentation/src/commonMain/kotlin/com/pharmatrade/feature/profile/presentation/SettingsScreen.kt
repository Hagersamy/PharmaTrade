package com.pharmatrade.feature.profile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.hints.DismissedHintsStore
import com.pharmatrade.core.common.i18n.AppLanguage
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.core.common.theme.ThemeManager
import com.pharmatrade.core.common.theme.ThemeMode
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.components.UploadRemindersEditor
import com.pharmatrade.core.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    // Supplied by the platform app (e.g. BuildConfig.VERSION_NAME on Android).
    appVersion: String,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val currentUser by SessionManager.currentUser.collectAsStateWithLifecycle()
    val reminderTimes by viewModel.reminderTimes.collectAsStateWithLifecycle()
    val currentLanguage by LanguageManager.language.collectAsState()
    val themeMode by ThemeManager.mode.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(title = strings.settingsTitle, onNavigateBack = onNavigateBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(icon = Icons.Filled.Language, title = strings.profileLanguage) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SegmentOption(
                        label = strings.profileLanguageEnglish,
                        selected = currentLanguage == AppLanguage.ENGLISH,
                        onClick = { LanguageManager.setLanguage(AppLanguage.ENGLISH) },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentOption(
                        label = strings.profileLanguageArabic,
                        selected = currentLanguage == AppLanguage.ARABIC,
                        onClick = { LanguageManager.setLanguage(AppLanguage.ARABIC) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SettingsCard(icon = Icons.Filled.Palette, title = strings.settingsAppearance) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SegmentOption(
                        label = strings.settingsThemeSystem,
                        icon = Icons.Filled.BrightnessAuto,
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { ThemeManager.setMode(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentOption(
                        label = strings.settingsThemeLight,
                        icon = Icons.Filled.LightMode,
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { ThemeManager.setMode(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentOption(
                        label = strings.settingsThemeDark,
                        icon = Icons.Filled.DarkMode,
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { ThemeManager.setMode(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // The Home tour exists for pharmacies and suppliers only (admins have their own dashboard).
            if (currentUser != null && currentUser?.userType != UserType.ADMIN) {
                SettingsCard(icon = Icons.Filled.TipsAndUpdates, title = strings.settingsAppTour) {
                    OutlinedButton(
                        onClick = {
                            DismissedHintsStore.reset(
                                DismissedHintsStore.HOME_TOUR_PHARMACY,
                                DismissedHintsStore.HOME_TOUR_SUPPLIER
                            )
                            // Back on Home, where the tour starts again.
                            onNavigateBack()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.settingsShowTourAgain, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (currentUser?.userType == UserType.SELLER) {
                SettingsCard(icon = Icons.Filled.NotificationsActive, title = strings.regUploadReminders) {
                    UploadRemindersEditor(
                        times = reminderTimes,
                        onAdd = viewModel::addReminder,
                        onRemove = viewModel::removeReminder
                    )
                }
            }
        }

        Text(
            text = "${strings.settingsAppVersion} $appVersion",
            style = MaterialTheme.typography.labelMedium,
            color = TextHint,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 16.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            HorizontalDivider(color = DividerGray)
            content()
        }
    }
}

@Composable
private fun SegmentOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PrimaryBlue else CardGray
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leadingIcon = if (selected) Icons.Filled.Check else icon
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = if (selected) Color.White else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else TextSecondary,
                maxLines = 1
            )
        }
    }
}
