package com.pharmatrade.core.common.i18n

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Mirrors SessionManager's shape (core/common/session/SessionManager.kt) — a process-wide
// singleton backed by its own prefs file, restored once at app start, read reactively via
// StateFlow so PharmaTradeTheme can recompose the whole tree the instant it changes.
object LanguageManager {
    private const val KEY_LANGUAGE = "app_language"

    private var settings: Settings? = null

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    // Non-Compose access to the current language's strings — for ViewModels (and other
    // non-Composable code) that need to localize a message, e.g. via Strings.friendlyError().
    val strings: Strings
        get() = if (_language.value == AppLanguage.ARABIC) ArabicStrings else EnglishStrings

    fun init(settings: Settings) {
        this.settings = settings
        // Only fall back to the device's system language on first launch (no saved choice yet)
        // — once the user picks a language in Profile, that explicit choice always wins.
        val savedCode = settings.getStringOrNull(KEY_LANGUAGE)
        _language.value = AppLanguage.fromCode(savedCode ?: systemLanguageCode())
    }

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        settings?.putString(KEY_LANGUAGE, language.code)
    }
}
