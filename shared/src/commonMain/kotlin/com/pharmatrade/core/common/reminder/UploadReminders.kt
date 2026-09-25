package com.pharmatrade.core.common.reminder

import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.session.SessionManager
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// A daily time-of-day (24h clock) at which a seller agent wants to be reminded to upload their
// inventory data.
@Serializable
data class ReminderTime(val hour: Int, val minute: Int) : Comparable<ReminderTime> {
    override fun compareTo(other: ReminderTime): Int = compareValuesBy(this, other, { it.hour }, { it.minute })

    fun format(amLabel: String, pmLabel: String): String {
        val hour12 = if (hour % 12 == 0) 12 else hour % 12
        return "$hour12:${minute.toString().padStart(2, '0')} ${if (hour < 12) amLabel else pmLabel}"
    }

    companion object {
        const val MAX_PER_DAY = 6
    }
}

// Platform hook that actually fires the local notifications (AlarmManager on Android). Each call to
// schedule() replaces whatever was scheduled before.
interface UploadReminderScheduler {
    fun schedule(times: List<ReminderTime>)
    fun cancelAll()
}

// Reminder times chosen at registration or later on Profile, stored per account (keyed by the login
// phone number, the only identifier guaranteed at sign-up — a pending registration may come back
// without a user id) so they only ever fire for the seller who set them — logging in as a different
// account on the same device won't pick them up. Kept on-device only; the backend has no field for this.
object UploadReminderStore {
    private const val KEY_REMINDERS = "upload_reminders_by_account"

    private var settings: Settings? = null
    private val byAccount = MutableStateFlow<Map<String, List<ReminderTime>>>(emptyMap())

    fun init(settings: Settings) {
        this.settings = settings
        byAccount.value = settings.getStringOrNull(KEY_REMINDERS)
            ?.let { runCatching { Json.decodeFromString<Map<String, List<ReminderTime>>>(it) }.getOrNull() }
            ?: emptyMap()
    }

    fun observe(phone: String): Flow<List<ReminderTime>> =
        byAccount.map { it[accountKey(phone)].orEmpty() }.distinctUntilChanged()

    // Keeps the reminders attached to the account when the seller edits their phone number.
    fun changePhone(oldPhone: String, newPhone: String) {
        val times = byAccount.value[accountKey(oldPhone)] ?: return
        if (accountKey(oldPhone) == accountKey(newPhone)) return
        save(oldPhone, emptyList())
        save(newPhone, times)
    }

    fun save(phone: String, times: List<ReminderTime>) {
        val key = accountKey(phone)
        val updated = if (times.isEmpty()) byAccount.value - key else byAccount.value + (key to times.distinct().sorted())
        byAccount.value = updated
        settings?.putString(KEY_REMINDERS, Json.encodeToString(updated))
    }

    // Keeps the platform scheduler in step with the session: reminders run only while an approved
    // seller agent who has saved times is logged in, and are cancelled on logout, account switch, or
    // while the account is still pending/declined (nothing to upload before approval).
    fun startSync(scope: CoroutineScope, scheduler: UploadReminderScheduler) {
        combine(SessionManager.currentUser, byAccount) { user, reminders ->
            if (user != null && user.userType == UserType.SELLER && !user.isPending && !user.isDeclined) {
                reminders[accountKey(user.phone)].orEmpty()
            } else {
                emptyList()
            }
        }
            .onEach { times -> if (times.isEmpty()) scheduler.cancelAll() else scheduler.schedule(times) }
            .launchIn(scope)
    }
}

// "+20 100 123 4567", "01001234567" and "201001234567" all refer to the same account: compare on
// the trailing 10 digits only.
private fun accountKey(phone: String): String = phone.filter { it.isDigit() }.takeLast(10)
