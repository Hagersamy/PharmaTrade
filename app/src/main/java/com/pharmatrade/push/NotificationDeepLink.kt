package com.pharmatrade.push

import android.content.Intent

data class NotificationDeepLink(val type: String, val notifiableId: String?)

fun Intent?.toNotificationDeepLink(): NotificationDeepLink? {
    val type = this?.getStringExtra(PharmaFirebaseMessagingService.EXTRA_NOTIFICATION_TYPE) ?: return null
    return NotificationDeepLink(
        type = type,
        notifiableId = this.getStringExtra(PharmaFirebaseMessagingService.EXTRA_NOTIFIABLE_ID)
    )
}
