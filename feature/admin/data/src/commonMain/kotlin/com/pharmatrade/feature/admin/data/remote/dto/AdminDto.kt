package com.pharmatrade.feature.admin.data.remote.dto

import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.dto.rawIntOrZero
import com.pharmatrade.core.network.dto.rawStringOrNull
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.model.RegistrationStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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
    // As of 2026-10-02 the registration-requests list no longer embeds any image — it only flags
    // that one exists. The image comes from admin/registration-requests/{id}/licence-image-url, which
    // answers JSON with an inline data_url (the sibling /licence-image route 404s for these requests).
    @SerialName("has_licence_image") val hasLicenceImage: Boolean? = null,
    // Supplier-only
    @SerialName("min_order_value") val minOrderValue: JsonElement? = null,
    @SerialName("min_order_qty") val minOrderQty: JsonElement? = null,
    @SerialName("zone_ids") val zoneIds: List<JsonElement>? = null,
    // ── entity_type=zone_update ──
    // NOTE: field names below are not from a confirmed sample yet — several likely spellings are
    // accepted, flat or nested under payload/metadata/details, with zones as bare ids or {id, name}
    // objects. The raw response is in logcat (Ktor Client, LogLevel.BODY) to confirm against.
    @SerialName("requester_type") val requesterType: String? = null,
    @SerialName("reason") val reason: String? = null,
    @SerialName("requested_zone_ids") val requestedZoneIds: List<JsonElement>? = null,
    @SerialName("requested_zones") val requestedZones: List<JsonElement>? = null,
    @SerialName("current_zone_ids") val currentZoneIds: List<JsonElement>? = null,
    @SerialName("current_zones") val currentZones: List<JsonElement>? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("payload") val payload: JsonObject? = null,
    @SerialName("metadata") val metadata: JsonObject? = null,
    @SerialName("details") val details: JsonObject? = null
) {
    // Primary image first (shown as "front"), the next one as "back".
    private val orderedImages: List<LicenceImageDto>
        get() = licenceImages.orEmpty().sortedByDescending { it.isPrimary == true }

    private val isZoneUpdate: Boolean get() = entityType?.lowercase() == "zone_update"

    private val nested: List<JsonObject> get() = listOfNotNull(payload, metadata, details)

    private fun nestedArray(vararg keys: String): List<JsonElement>? =
        nested.firstNotNullOfOrNull { obj -> keys.firstNotNullOfOrNull { (obj[it] as? JsonArray)?.toList() } }

    // Loaded by Coil with the session's Bearer token; PharmaTradeApp's interceptor unwraps the JSON
    // response's data_url into image bytes Coil can decode.
    private fun registrationLicenceImageUrl(): String? {
        if (hasLicenceImage != true) return null
        val requestId = id.rawStringOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return "${ApiClient.baseUrl.trimEnd('/')}/admin/registration-requests/$requestId/licence-image-url"
    }

    // licence-image-url only ever serves the primary (front) image, and the list doesn't carry the
    // back image's id. The request's detail endpoint lists every image, so point at that; the
    // licence_side marker (ignored by the backend) tells PharmaTradeApp's interceptor to pick the
    // non-primary entry out of its licence_images array.
    private fun registrationLicenceBackUrl(): String? {
        if (hasLicenceImage != true) return null
        val requestId = id.rawStringOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return "${ApiClient.baseUrl.trimEnd('/')}/admin/registration-requests/$requestId?licence_side=back"
    }

    private fun nestedString(vararg keys: String): String? =
        nested.firstNotNullOfOrNull { obj -> keys.firstNotNullOfOrNull { (obj[it] as? JsonPrimitive)?.contentOrNull } }

    fun toPendingUser(defaultType: UserType = UserType.BUYER): PendingUser {
        // For a zone update, entity_type describes the request, not who sent it.
        val resolvedRole = if (isZoneUpdate) requesterType ?: role ?: userType ?: type
            else entityType ?: role ?: userType ?: type
        val requested = if (isZoneUpdate) {
            requestedZoneIds ?: requestedZones ?: zoneIds
                ?: nestedArray("requested_zone_ids", "requested_zones", "zone_ids", "zones")
        } else null
        val current = if (isZoneUpdate) {
            currentZoneIds ?: currentZones ?: nestedArray("current_zone_ids", "current_zones", "old_zone_ids")
        } else null
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
            licenceFrontUrl = orderedImages.getOrNull(0)?.source()
                ?: licenceFrontRaw.toLicenceUrl()
                ?: registrationLicenceImageUrl(),
            licenceBackUrl = orderedImages.getOrNull(1)?.source()
                ?: licenceBackRaw.toLicenceUrl()
                ?: registrationLicenceBackUrl(),
            minOrderValue = minOrderValue.rawStringOrNull(),
            minOrderQty = minOrderQty.rawStringOrNull(),
            additionalZoneIds = if (isZoneUpdate) emptyList() else zoneIds?.map { it.rawStringOrNull() ?: "" } ?: emptyList(),
            isZoneUpdate = isZoneUpdate,
            zoneUpdateReason = if (isZoneUpdate) reason ?: nestedString("reason", "notes") else null,
            requestedZoneIds = requested.orEmpty().mapNotNull { it.zoneId() },
            currentZoneIds = current.orEmpty().mapNotNull { it.zoneId() },
            serverZoneNames = (requested.orEmpty() + current.orEmpty()).mapNotNull { it.zoneIdToName() }.toMap(),
            submittedAt = submittedAt ?: createdAt
        )
    }
}

// A zone may arrive as a bare id (3 / "3") or an object ({"id": 3, "name": "Nasr City"}).
private fun JsonElement.zoneId(): String? = when (this) {
    is JsonObject -> this["id"]?.rawStringOrNull()
    else -> rawStringOrNull()
}?.takeIf { it.isNotBlank() }

private fun JsonElement.zoneIdToName(): Pair<String, String>? {
    val obj = this as? JsonObject ?: return null
    val id = zoneId() ?: return null
    val name = (obj["name"] as? JsonPrimitive)?.contentOrNull ?: return null
    return id to name
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
