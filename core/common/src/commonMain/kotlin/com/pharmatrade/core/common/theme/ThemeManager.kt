package com.pharmatrade.core.common.theme

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

// Same shape as LanguageManager: process-wide, restored once at app start, observed by
// PharmaTradeTheme so switching mode in Settings recomposes the whole tree immediately.
object ThemeManager {
    private const val KEY_THEME_MODE = "app_theme_mode"

    private var settings: Settings? = null

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun init(settings: Settings) {
        this.settings = settings
        _mode.value = settings.getStringOrNull(KEY_THEME_MODE)
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM
    }

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        settings?.putString(KEY_THEME_MODE, mode.name)
    }
}
