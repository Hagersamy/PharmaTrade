package com.pharmatrade.feature.admin.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.model.RegistrationStats

data class PendingUsersPageDto(
    @SerializedName("data") val items: List<PendingUserDto>? = null,
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("total") val total: Int? = null
)

data class PendingUserDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("business_name") val businessName: String? = null,
    @SerializedName("licence_number") val licenceNumber: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("zone_id") val zoneId: Any? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("entity_type") val entityType: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("user_type") val userType: String? = null,
    @SerializedName("type") val type: String? = null,
    // Licence images — server may return a full URL string OR just the numeric ID
    @SerializedName("licence_image") val licenceFrontRaw: Any? = null,
    @SerializedName("licence_image_back") val licenceBackRaw: Any? = null,
    // Supplier-only
    @SerializedName("min_order_value") val minOrderValue: Any? = null,
    @SerializedName("min_order_qty") val minOrderQty: Any? = null,
    @SerializedName("zone_ids") val zoneIds: List<Any>? = null
) {
    fun toPendingUser(defaultType: UserType = UserType.BUYER): PendingUser {
        val resolvedRole = entityType ?: role ?: userType ?: type
        val ut = when (resolvedRole?.lowercase()) {
            "supplier", "seller", "agent", "drug_seller_agent" -> UserType.SELLER
            "pharmacy", "buyer", "pharmacist" -> UserType.BUYER
            else -> defaultType
        }
        return PendingUser(
            id = id?.toString() ?: "",
            name = name ?: "",
            email = email ?: "",
            phone = phone ?: "",
            businessName = businessName ?: "",
            licenceNumber = licenceNumber,
            address = address,
            zoneId = zoneId?.toString(),
            userType = ut,
            status = status ?: "pending",
            licenceFrontUrl = licenceFrontRaw?.toLicenceUrl(),
            licenceBackUrl = licenceBackRaw?.toLicenceUrl(),
            minOrderValue = minOrderValue?.toString(),
            minOrderQty = minOrderQty?.toString(),
            additionalZoneIds = zoneIds?.map { it.toString() } ?: emptyList()
        )
    }
}

private const val BASE = "https://unbuckled-word-defuse.ngrok-free.dev/api/v1/"

/**
 * Converts whatever the server sends for a licence image to a usable HTTPS URL.
 * - Integer / numeric string → build the admin view URL by ID (Bearer token auth)
 * - String URL (localhost or 127.0.0.1) → replace host with ngrok domain
 * - Already an ngrok/external URL → force HTTPS
 */
private fun Any.toLicenceUrl(): String? {
    val raw = toString().trim()
    if (raw.isBlank() || raw == "null") return null

    // If it's a numeric ID, build the view endpoint URL
    if (raw.toDoubleOrNull() != null) {
        val id = raw.toBigDecimal().toLong()
        return "${BASE}admin/licences/$id/view"
    }

    // It's a string URL — normalise the host
    return raw
        .replace(Regex("https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?/"),
            BASE)
        .replace("http://unbuckled-word-defuse.ngrok-free.dev",
            "https://unbuckled-word-defuse.ngrok-free.dev")
}

data class RegistrationStatsDto(
    @SerializedName("total") val total: Any? = null,
    @SerializedName("pending") val pending: Any? = null,
    @SerializedName("approved") val approved: Any? = null,
    @SerializedName("declined") val declined: Any? = null,
    @SerializedName("rejected") val rejected: Any? = null,
    @SerializedName("pending_pharmacies") val pendingPharmacies: Any? = null,
    @SerializedName("pending_suppliers") val pendingSuppliers: Any? = null
) {
    fun toStats() = RegistrationStats(
        pending = pending?.toString()?.toIntOrNull() ?: 0,
        approved = approved?.toString()?.toIntOrNull() ?: 0,
        declined = (declined ?: rejected)?.toString()?.toIntOrNull() ?: 0,
        total = total?.toString()?.toIntOrNull() ?: 0,
        pendingPharmacies = pendingPharmacies?.toString()?.toIntOrNull() ?: 0,
        pendingSuppliers = pendingSuppliers?.toString()?.toIntOrNull() ?: 0
    )
}
