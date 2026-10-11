package com.pharmatrade.core.common.search

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// The user's recent drug searches, newest first. Kept on-device only (the backend has no search
// history endpoint) and stored per account, so a different login on the same phone doesn't see
// someone else's searches. Same shape as UploadReminderStore.
object SearchHistoryStore {
    const val MAX_ENTRIES = 10
    private const val KEY_HISTORY = "search_history_by_account"

    private var settings: Settings? = null
    private val byAccount = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    fun init(settings: Settings) {
        this.settings = settings
        byAccount.value = settings.getStringOrNull(KEY_HISTORY)
            ?.let { runCatching { Json.decodeFromString<Map<String, List<String>>>(it) }.getOrNull() }
            ?: emptyMap()
    }

    fun observe(account: String): Flow<List<String>> =
        byAccount.map { it[account].orEmpty() }.distinctUntilChanged()

    // Moves [query] to the top; a repeat search (any letter case) replaces its older entry.
    fun add(account: String, query: String) {
        val entry = query.trim()
        if (entry.isEmpty()) return
        val current = byAccount.value[account].orEmpty()
        save(account, (listOf(entry) + current.filterNot { it.equals(entry, ignoreCase = true) }).take(MAX_ENTRIES))
    }

    fun remove(account: String, query: String) {
        save(account, byAccount.value[account].orEmpty() - query)
    }

    fun clear(account: String) = save(account, emptyList())

    private fun save(account: String, entries: List<String>) {
        val updated = if (entries.isEmpty()) byAccount.value - account else byAccount.value + (account to entries)
        byAccount.value = updated
        settings?.putString(KEY_HISTORY, Json.encodeToString(updated))
    }
}
