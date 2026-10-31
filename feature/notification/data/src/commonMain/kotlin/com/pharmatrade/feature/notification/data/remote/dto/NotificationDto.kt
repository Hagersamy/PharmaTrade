package com.pharmatrade.feature.notification.data.remote.dto

import com.pharmatrade.core.network.dto.rawIntOrZero
import com.pharmatrade.core.network.dto.rawStringOrNull
import com.pharmatrade.feature.notification.domain.model.Notification
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class NotificationDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("notifiable_type") val notifiableType: String? = null,
    @SerialName("notifiable_id") val notifiableId: JsonElement? = null,
    @SerialName("is_read") val isRead: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    // Laravel's stored-notification shape: the push payload ("type", "notifiable_id", ...) sits
    // under "data", and the top-level "type" is the PHP class name.
    // JsonElement, not JsonObject: a non-object "data" must not fail the whole list.
    @SerialName("data") val data: JsonElement? = null
) {
    private fun dataString(key: String) =
        ((data as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    // A class name like "App\Notifications\DrugsMissingEnglish" isn't a routable type.
    private val isClassName: Boolean get() = type?.contains('\\') == true

    fun toDomain() = Notification(
        id = id.rawStringOrNull() ?: id.rawIntOrZero().toString(),
        title = title ?: dataString("title") ?: "",
        body = body ?: dataString("body") ?: "",
        type = (if (isClassName) dataString("type") else type) ?: dataString("type") ?: type ?: "general",
        notifiableType = notifiableType ?: dataString("notifiable_type"),
        notifiableId = notifiableId.rawStringOrNull() ?: dataString("notifiable_id"),
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
