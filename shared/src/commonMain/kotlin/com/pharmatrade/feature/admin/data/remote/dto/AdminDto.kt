package com.pharmatrade.feature.admin.data.remote.dto

import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.network.dto.rawIntOrZero
import com.pharmatrade.core.network.dto.rawStringOrNull
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.model.RegistrationStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class PendingUsersPageDto(
    @SerialName("data") val items: List<PendingUserDto>? = null,
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("total") val total: Int? = null
)

@Serializable
data class PendingUserDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("business_name") val businessName: String? = null,
    @SerialName("licence_number") val licenceNumber: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("zone_id") val zoneId: JsonElement? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("entity_type") val entityType: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("user_type") val userType: String? = null,
    @SerialName("type") val type: String? = null,
    // Licence images — server may return a full URL string OR just the numeric ID
    @SerialName("licence_image") val licenceFrontRaw: JsonElement? = null,
    @SerialName("licence_image_back") val licenceBackRaw: JsonElement? = null,
    // Supplier-only
    @SerialName("min_order_value") val minOrderValue: JsonElement? = null,
    @SerialName("min_order_qty") val minOrderQty: JsonElement? = null,
    @SerialName("zone_ids") val zoneIds: List<JsonElement>? = null
) {
    fun toPendingUser(defaultType: UserType = UserType.BUYER): PendingUser {
        val resolvedRole = entityType ?: role ?: userType ?: type
        val ut = when (resolvedRole?.lowercase()) {
            "supplier", "seller", "agent", "drug_seller_agent" -> UserType.SELLER
            "pharmacy", "buyer", "pharmacist" -> UserType.BUYER
            else -> defaultType
        }
        return PendingUser(
            id = id.rawStringOrNull() ?: "",
            name = name ?: "",
            email = email ?: "",
            phone = phone ?: "",
            businessName = businessName ?: "",
            licenceNumber = licenceNumber,
            address = address,
            zoneId = zoneId.rawStringOrNull(),
            userType = ut,
            status = status ?: "pending",
            licenceFrontUrl = licenceFrontRaw.toLicenceUrl(),
            licenceBackUrl = licenceBackRaw.toLicenceUrl(),
            minOrderValue = minOrderValue.rawStringOrNull(),
            minOrderQty = minOrderQty.rawStringOrNull(),
            additionalZoneIds = zoneIds?.map { it.rawStringOrNull() ?: "" } ?: emptyList()
        )
    }
}

private const val BASE = "https://pharma-trade-backend-production-e64c.up.railway.app/api/v1/"

/**
 * Converts whatever the server sends for a licence image to a usable HTTPS URL.
 * - Integer / numeric string → build the admin view URL by ID (Bearer token auth)
 * - String URL (localhost or 127.0.0.1) → replace host with ngrok domain
 * - Already an ngrok/external URL → force HTTPS
 */
private fun JsonElement?.toLicenceUrl(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    val raw = primitive.content.trim()
    if (raw.isBlank() || raw == "null") return null

    // If it's a numeric ID, build the view endpoint URL
    val numericId = primitive.longOrNull ?: raw.toDoubleOrNull()?.toLong()
    if (numericId != null) {
        return "${BASE}admin/licences/$numericId/view"
    }

    // It's a string URL — normalise the host
    return raw
        .replace(
            Regex("https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?/"),
            BASE
        )
        .replace(
            "http://pharma-trade-backend-production-e64c.up.railway.app",
            "https://pharma-trade-backend-production-e64c.up.railway.app"
        )
}

@Serializable
data class RegistrationStatsDto(
    @SerialName("total") val total: JsonElement? = null,
    @SerialName("pending") val pending: JsonElement? = null,
    @SerialName("approved") val approved: JsonElement? = null,
    @SerialName("declined") val declined: JsonElement? = null,
    @SerialName("rejected") val rejected: JsonElement? = null,
    @SerialName("pending_pharmacies") val pendingPharmacies: JsonElement? = null,
    @SerialName("pending_suppliers") val pendingSuppliers: JsonElement? = null
) {
    fun toStats() = RegistrationStats(
        pending = pending.rawIntOrZero(),
        approved = approved.rawIntOrZero(),
        declined = if (declined != null) declined.rawIntOrZero() else rejected.rawIntOrZero(),
        total = total.rawIntOrZero(),
        pendingPharmacies = pendingPharmacies.rawIntOrZero(),
        pendingSuppliers = pendingSuppliers.rawIntOrZero()
    )
}
