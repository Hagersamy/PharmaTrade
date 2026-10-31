package com.pharmatrade.push

import android.content.Intent

data class NotificationDeepLink(val type: String, val notifiableId: String?)

// Two ways a push tap reaches MainActivity:
//  - our own notification (built in PharmaFirebaseMessagingService while the app runs or for
//    data-only pushes) carries EXTRA_NOTIFICATION_TYPE / EXTRA_NOTIFIABLE_ID;
//  - a push with a "notification" block that arrives while the app is in the background is shown
//    by the system without our code running, and its tap carries the FCM data keys as they are
//    ("type", "notifiable_id").
fun Intent?.toNotificationDeepLink(): NotificationDeepLink? {
    val intent = this ?: return null
    val type = intent.extraString(PharmaFirebaseMessagingService.EXTRA_NOTIFICATION_TYPE)
        ?: intent.extraString(FCM_DATA_TYPE)
        ?: return null
    return NotificationDeepLink(
        type = type,
        notifiableId = intent.extraString(PharmaFirebaseMessagingService.EXTRA_NOTIFIABLE_ID)
            ?: intent.extraString(FCM_DATA_NOTIFIABLE_ID)
    )
}

private const val FCM_DATA_TYPE = "type"
private const val FCM_DATA_NOTIFIABLE_ID = "notifiable_id"

// FCM data values are strings, but read any extra type so a stray number doesn't drop the link.
private fun Intent.extraString(key: String): String? =
    extras?.get(key)?.toString()?.takeIf { it.isNotBlank() }
