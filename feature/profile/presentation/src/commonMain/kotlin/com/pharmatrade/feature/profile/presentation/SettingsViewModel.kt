package com.pharmatrade.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.reminder.ReminderTime
import com.pharmatrade.core.common.reminder.UploadReminderStore
import com.pharmatrade.core.common.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

// Language and theme live in their app-wide managers and are read directly by the screen; this
// ViewModel only owns the seller agent's daily "upload your data" reminders (on-device only,
// applied immediately).
class SettingsViewModel : ViewModel() {

    private val _reminderTimes = MutableStateFlow<List<ReminderTime>>(emptyList())
    val reminderTimes: StateFlow<List<ReminderTime>> = _reminderTimes.asStateFlow()

    init {
        if (SessionManager.user?.userType == UserType.SELLER) {
            observeReminders()
        }
    }

    // Follows the session's phone so the list stays right after the seller edits their number.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeReminders() {
        SessionManager.currentUser
            .map { it?.phone }
            .distinctUntilChanged()
            .flatMapLatest { phone -> if (phone == null) flowOf(emptyList()) else UploadReminderStore.observe(phone) }
            .onEach { times -> _reminderTimes.update { times } }
            .launchIn(viewModelScope)
    }

    fun addReminder(time: ReminderTime) {
        val phone = SessionManager.user?.phone ?: return
        val current = _reminderTimes.value
        if (time in current || current.size >= ReminderTime.MAX_PER_DAY) return
        UploadReminderStore.save(phone, current + time)
    }

    fun removeReminder(time: ReminderTime) {
        val phone = SessionManager.user?.phone ?: return
        UploadReminderStore.save(phone, _reminderTimes.value - time)
    }
}
