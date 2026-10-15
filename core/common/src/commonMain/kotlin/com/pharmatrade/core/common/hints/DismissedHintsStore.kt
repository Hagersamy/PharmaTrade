package com.pharmatrade.core.common.hints

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// One-time tips the user has closed, kept on-device so they stay hidden after a restart.
// Without init() (e.g. desktop) dismissals only last for the session.
object DismissedHintsStore {
    const val MARKETPLACE_BROWSE = "marketplace_browse"
    private const val KEY_PREFIX = "hint_dismissed_"

    private var settings: Settings? = null
    private val dismissed = MutableStateFlow<Set<String>>(emptySet())

    fun init(settings: Settings) {
        this.settings = settings
        dismissed.value = settings.keys
            .filter { it.startsWith(KEY_PREFIX) && settings.getBoolean(it, false) }
            .map { it.removePrefix(KEY_PREFIX) }
            .toSet()
    }

    fun observe(hint: String): Flow<Boolean> = dismissed.map { hint in it }.distinctUntilChanged()

    fun dismiss(hint: String) {
        dismissed.value = dismissed.value + hint
        settings?.putBoolean(KEY_PREFIX + hint, true)
    }
}
