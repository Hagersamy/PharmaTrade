package com.pharmatrade.feature.admin.data.remote.dto

import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.network.ApiClient
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
    // GET admin/registration-requests (confirmed 2026-09-27) sends images as this array instead of
    // the two fields above: each has an inline base64 data_url (sometimes null) and an authed url.
    @SerialName("licence_images") val licenceImages: List<LicenceImageDto>? = null,
    // Supplier-only
    @SerialName("min_order_value") val minOrderValue: JsonElement? = null,
    @SerialName("min_order_qty") val minOrderQty: JsonElement? = null,
    @SerialName("zone_ids") val zoneIds: List<JsonElement>? = null
) {
    // Primary image first (shown as "front"), the next one as "back".
    private val orderedImages: List<LicenceImageDto>
        get() = licenceImages.orEmpty().sortedByDescending { it.isPrimary == true }

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
            licenceFrontUrl = orderedImages.getOrNull(0)?.source() ?: licenceFrontRaw.toLicenceUrl(),
            licenceBackUrl = orderedImages.getOrNull(1)?.source() ?: licenceBackRaw.toLicenceUrl(),
            minOrderValue = minOrderValue.rawStringOrNull(),
            minOrderQty = minOrderQty.rawStringOrNull(),
            additionalZoneIds = zoneIds?.map { it.rawStringOrNull() ?: "" } ?: emptyList()
        )
    }
}

@Serializable
data class LicenceImageDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("is_primary") val isPrimary: Boolean? = null,
    @SerialName("data_url") val dataUrl: String? = null,
    @SerialName("url") val url: String? = null
) {
    // Inline base64 needs no extra request or auth, so prefer it; otherwise the authed url, which
    // Coil loads with the session token (see PharmaTradeApp.setupCoil).
    fun source(): String? =
        dataUrl?.takeIf { it.startsWith("data:") }
            ?: url?.let { JsonPrimitive(it).toLicenceUrl() }
}

/**
 * Converts whatever the server sends for a licence image to a URL the app can actually load.
 * Always resolved against the backend this build talks to (ApiClient.baseUrl — ngrok for dev,
 * Railway for prod), never a hardcoded host: the image request carries this session's token,
 * which is only valid on the backend that issued it.
 * - Integer / numeric string → admin view endpoint by ID (Bearer token auth, sent by Coil)
 * - Absolute URL on localhost / 127.0.0.1 / 10.0.2.2 or the backend's own host → rehosted onto
 *   the configured backend origin over HTTPS (Laravel builds these from APP_URL, which is often
 *   http:// or localhost, and Android blocks cleartext http)
 * - Relative path ("/storage/…", "licences/…") → prefixed with the backend origin
 * - Any other absolute URL (e.g. S3) → left as-is, only upgraded to HTTPS
 */
private fun JsonElement?.toLicenceUrl(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    val raw = primitive.content.trim().replace("\\/", "/")
    if (raw.isBlank() || raw == "null") return null

    val apiBase = ApiClient.baseUrl.trimEnd('/') + "/"
    val origin = Regex("^https?://[^/]+").find(apiBase)?.value ?: return null
    val backendHost = origin.substringAfter("://")

    val numericId = primitive.longOrNull ?: raw.toLongOrNull()
    if (numericId != null) {
        return "${apiBase}admin/licences/$numericId/view"
    }

    val absolute = Regex("^https?://([^/:]+)(:\\d+)?(/.*)?$").find(raw)
        ?: return "$origin/${raw.trimStart('/')}"

    val host = absolute.groupValues[1]
    val path = absolute.groupValues[3]
    return if (host in setOf("localhost", "127.0.0.1", "10.0.2.2", backendHost)) {
        "$origin$path"
    } else {
        raw.replaceFirst("http://", "https://")
    }
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
