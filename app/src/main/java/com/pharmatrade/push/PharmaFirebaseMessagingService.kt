package com.pharmatrade.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pharmatrade.MainActivity
import com.pharmatrade.R

// Data-only pushes are assumed (see project-pharmatrade-fcm-scope memory) so this always builds
// the notification itself from `message.data`, using the same title/body/type/notifiable_id shape
// as the REST notification list (feature/notification), rather than relying on FCM's own
// notification-payload auto-display.
class PharmaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // The backend only accepts device_token on login/register (see AuthRepositoryImpl), so a
        // rotation here doesn't reach it until the next login — nothing to actively do in that
        // case. Logged so the current token can still be copied into Firebase console's "Send
        // test message" tool for manual testing.
        Log.i(TAG, "New FCM device token: $token")
        PendingDeviceToken.latest = token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "PharmaTrade"
        val body = data["body"] ?: message.notification?.body ?: ""
        val type = data["type"] ?: "general"
        val notifiableId = data["notifiable_id"]
        showNotification(title = title, body = body, type = type, notifiableId = notifiableId)
    }

    private fun showNotification(title: String, body: String, type: String, notifiableId: String?) {
        ensureChannel()

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, type)
            notifiableId?.let { putExtra(EXTRA_NOTIFIABLE_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted — dropping push (type=$type)")
            return
        }
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Order & account updates", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        private const val TAG = "PharmaFCM"

        // Matches app/src/main/res/values/strings.xml's default_notification_channel_id, and the
        // AndroidManifest com.google.firebase.messaging.default_notification_channel_id meta-data —
        // kept identical so a system-auto-displayed notification-payload push (if the backend ever
        // sends one) and our own data-only pushes land in the same channel.
        const val CHANNEL_ID = "pharmatrade_updates"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_NOTIFIABLE_ID = "notifiable_id"
    }
}
