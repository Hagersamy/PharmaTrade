package com.pharmatrade.feature.notification.data.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.notification.data.remote.NotificationApi
import com.pharmatrade.feature.notification.domain.model.NotificationsPage
import com.pharmatrade.feature.notification.domain.repository.NotificationRepository
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NotificationRepositoryImpl(
    private val api: NotificationApi = NotificationApi()
) : NotificationRepository {

    override suspend fun getNotifications(page: Int?, perPage: Int?): Result<NotificationsPage> = try {
        val response = api.getNotifications(page = page, perPage = perPage)
        val notifications = response.data?.data?.map { it.toDomain() } ?: emptyList()
        val total = response.data?.total ?: notifications.size
        Result.Success(NotificationsPage(notifications = notifications, total = total))
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load notifications", e)
    }

    override suspend fun getUnreadCount(): Result<Int> = try {
        val response = api.getUnreadCount()
        Result.Success(response.data?.count ?: 0)
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load unread count", e)
    }

    override suspend fun markAsRead(id: String): Result<Unit> = try {
        api.markAsRead(id)
        Result.Success(Unit)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark notification as read", e)
    }

    override suspend fun markAllAsRead(): Result<Int> = try {
        val response = api.markAllAsRead()
        Result.Success(response.data?.updatedCount ?: 0)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark notifications as read", e)
    }

    override suspend fun deleteNotification(id: String): Result<Unit> = try {
        api.deleteNotification(id)
        Result.Success(Unit)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to delete notification", e)
    }

    override suspend fun clearAll(): Result<Int> = try {
        val response = api.clearAll()
        Result.Success(response.data?.deletedCount ?: 0)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to clear notifications", e)
    }

    private suspend fun parseHttpError(e: ResponseException): String {
        val code = e.response.status.value
        val body = runCatching { e.response.bodyAsText() }.getOrNull()
        if (body.isNullOrBlank()) return "Server error ($code)"
        return runCatching {
            val json = Json.parseToJsonElement(body).jsonObject
            val message = json["message"]?.jsonPrimitive?.contentOrNull
            val errors = json["errors"] as? JsonObject
            if (errors != null && errors.isNotEmpty()) {
                val fieldMsg = errors.values.firstOrNull()?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                fieldMsg ?: message ?: "Server error ($code)"
            } else {
                message ?: "Server error ($code)"
            }
        }.getOrDefault("Server error ($code)")
    }
}
