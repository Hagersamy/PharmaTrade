package com.pharmatrade.feature.notification.data.remote.dto

import com.pharmatrade.core.network.dto.rawIntOrZero
import com.pharmatrade.core.network.dto.rawStringOrNull
import com.pharmatrade.feature.notification.domain.model.Notification
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NotificationDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("notifiable_type") val notifiableType: String? = null,
    @SerialName("notifiable_id") val notifiableId: JsonElement? = null,
    @SerialName("is_read") val isRead: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toDomain() = Notification(
        id = id.rawStringOrNull() ?: id.rawIntOrZero().toString(),
        title = title ?: "",
        body = body ?: "",
        type = type ?: "general",
        notifiableType = notifiableType,
        notifiableId = notifiableId.rawStringOrNull(),
        isRead = isRead ?: false,
        createdAt = createdAt ?: ""
    )
}

@Serializable
data class NotificationPageDto(
    @SerialName("data") val data: List<NotificationDto>? = null,
    @SerialName("total") val total: Int? = null,
    @SerialName("current_page") val currentPage: Int? = null
)

@Serializable
data class UnreadCountDto(
    @SerialName("count") val count: Int? = null
)

@Serializable
data class MarkAllReadDto(
    @SerialName("updated_count") val updatedCount: Int? = null
)

@Serializable
data class DeletedCountDto(
    @SerialName("deleted_count") val deletedCount: Int? = null
)
