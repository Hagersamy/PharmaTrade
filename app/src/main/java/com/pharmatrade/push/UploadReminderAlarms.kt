package com.pharmatrade.push

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pharmatrade.MainActivity
import com.pharmatrade.R
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.reminder.ReminderTime
import com.pharmatrade.core.common.reminder.UploadReminderScheduler
import java.util.Calendar

// Local (on-device) daily reminders for a seller agent to upload their inventory — no backend push
// involved. One alarm per reminder time; each fires once and re-arms itself for the next day, which
// keeps working through Doze without needing the exact-alarm permission.
class AndroidUploadReminderScheduler(context: Context) : UploadReminderScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun schedule(times: List<ReminderTime>) {
        cancelAll()
        times.take(ReminderTime.MAX_PER_DAY).forEachIndexed { slot, time -> scheduleNext(slot, time) }
    }

    override fun cancelAll() {
        // Slots are 0 until MAX_PER_DAY, so every alarm this app could have set can be looked up
        // without remembering what was scheduled last time.
        repeat(ReminderTime.MAX_PER_DAY) { slot ->
            pendingIntent(slot, time = null, flags = PendingIntent.FLAG_NO_CREATE)?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    fun scheduleNext(slot: Int, time: ReminderTime) {
        val pending = pendingIntent(slot, time, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val triggerAt = nextTriggerMillis(time)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            // Without SCHEDULE_EXACT_ALARM (Android 12+) the system may shift this by a few minutes.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pendingIntent(slot: Int, time: ReminderTime?, flags: Int): PendingIntent? {
        val intent = Intent(appContext, UploadReminderReceiver::class.java).apply {
            action = ACTION_UPLOAD_REMINDER
            putExtra(EXTRA_SLOT, slot)
            time?.let {
                putExtra(EXTRA_HOUR, it.hour)
                putExtra(EXTRA_MINUTE, it.minute)
            }
        }
        return PendingIntent.getBroadcast(appContext, REQUEST_CODE_BASE + slot, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun nextTriggerMillis(time: ReminderTime): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    companion object {
        const val ACTION_UPLOAD_REMINDER = "com.pharmatrade.action.UPLOAD_REMINDER"
        const val EXTRA_SLOT = "reminder_slot"
        const val EXTRA_HOUR = "reminder_hour"
        const val EXTRA_MINUTE = "reminder_minute"
        private const val REQUEST_CODE_BASE = 7300
    }
}

class UploadReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getIntExtra(AndroidUploadReminderScheduler.EXTRA_SLOT, -1)
        val hour = intent.getIntExtra(AndroidUploadReminderScheduler.EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(AndroidUploadReminderScheduler.EXTRA_MINUTE, -1)
        if (slot < 0 || hour < 0 || minute < 0) return

        showNotification(context)
        // Re-arm for the same time tomorrow. If the seller has logged out since, PharmaTradeApp's
        // UploadReminderStore sync has already cancelled this slot and nothing reaches here.
        AndroidUploadReminderScheduler(context).scheduleNext(slot, ReminderTime(hour, minute))
    }

    private fun showNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted — skipping upload reminder")
            return
        }
        ensureChannel(context)

        val strings = LanguageManager.strings
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PharmaFirebaseMessagingService.EXTRA_NOTIFICATION_TYPE, NOTIFICATION_TYPE)
        }
        val contentIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(strings.uploadReminderNotificationTitle)
            .setContentText(strings.uploadReminderNotificationBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(strings.uploadReminderNotificationBody))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        // Fixed id: a newer reminder replaces an unread older one instead of stacking up.
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Upload reminders", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        private const val TAG = "PharmaReminder"
        private const val CHANNEL_ID = "pharmatrade_upload_reminders"
        private const val NOTIFICATION_ID = 7300

        // Routed to the inventory upload screen by AppNavigation's resolveNotificationDestination.
        const val NOTIFICATION_TYPE = "upload_reminder"
    }
}

// Alarms don't survive a reboot or an app update. Nothing to do here beyond waking the process:
// PharmaTradeApp.onCreate re-runs UploadReminderStore.startSync, which re-schedules them.
class UploadReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
