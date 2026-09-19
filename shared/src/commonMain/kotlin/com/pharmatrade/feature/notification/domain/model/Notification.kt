package com.pharmatrade.feature.notification.domain.model

data class Notification(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val notifiableType: String?,
    val notifiableId: String?,
    val isRead: Boolean,
    val createdAt: String
)

data class NotificationsPage(
    val notifications: List<Notification>,
    val total: Int
)
