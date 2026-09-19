package com.pharmatrade.feature.notification.data.remote

import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.dto.ApiResponse
import com.pharmatrade.feature.notification.data.remote.dto.DeletedCountDto
import com.pharmatrade.feature.notification.data.remote.dto.MarkAllReadDto
import com.pharmatrade.feature.notification.data.remote.dto.NotificationPageDto
import com.pharmatrade.feature.notification.data.remote.dto.UnreadCountDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch

class NotificationApi(private val client: HttpClient = ApiClient.httpClient) {

    suspend fun getNotifications(page: Int? = null, perPage: Int? = null): ApiResponse<NotificationPageDto> =
        client.get("notifications") {
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()

    suspend fun getUnreadCount(): ApiResponse<UnreadCountDto> =
        client.get("notifications/unread-count").body()

    suspend fun markAsRead(id: String) {
        client.patch("notifications/$id/read")
    }

    suspend fun markAllAsRead(): ApiResponse<MarkAllReadDto> =
        client.patch("notifications/read-all").body()

    suspend fun deleteNotification(id: String) {
        client.delete("notifications/$id")
    }

    suspend fun clearAll(): ApiResponse<DeletedCountDto> =
        client.delete("notifications").body()
}
