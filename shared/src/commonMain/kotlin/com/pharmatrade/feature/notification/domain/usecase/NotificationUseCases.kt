package com.pharmatrade.feature.notification.domain.usecase

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.notification.domain.model.NotificationsPage
import com.pharmatrade.feature.notification.domain.repository.NotificationRepository

class GetNotificationsUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(page: Int? = null, perPage: Int? = null): Result<NotificationsPage> =
        repository.getNotifications(page, perPage)
}

class GetUnreadNotificationCountUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(): Result<Int> = repository.getUnreadCount()
}

class MarkNotificationAsReadUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.markAsRead(id)
}

class MarkAllNotificationsAsReadUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(): Result<Int> = repository.markAllAsRead()
}

class DeleteNotificationUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteNotification(id)
}

class ClearAllNotificationsUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(): Result<Int> = repository.clearAll()
}
