package com.pharmatrade.feature.notification.domain.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.notification.domain.model.NotificationsPage

interface NotificationRepository {
    suspend fun getNotifications(page: Int? = null, perPage: Int? = null): Result<NotificationsPage>
    suspend fun getUnreadCount(): Result<Int>
    suspend fun markAsRead(id: String): Result<Unit>
    suspend fun markAllAsRead(): Result<Int>
    suspend fun deleteNotification(id: String): Result<Unit>
    suspend fun clearAll(): Result<Int>
}
