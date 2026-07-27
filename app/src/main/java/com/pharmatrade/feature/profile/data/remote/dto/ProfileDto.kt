package com.pharmatrade.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.pharmatrade.feature.auth.data.remote.dto.ZoneDto
import com.pharmatrade.feature.profile.domain.model.BranchProfile
import com.pharmatrade.feature.profile.domain.model.ProfileInfo
import com.pharmatrade.feature.profile.domain.model.SupplierProfile
import com.pharmatrade.feature.profile.domain.model.ZoneUpdateRequest

// Gson deserializes JSON numbers into Any-typed fields as Double, so a raw id from the
// backend can arrive as either a String or a Number depending on endpoint — handle both.
private fun Any?.toIntSafe(default: Int = 0): Int = when (this) {
    is Number -> toInt()
    is String -> toDoubleOrNull()?.toInt() ?: default
    else -> default
}

// ── GET /profile ───────────────────────────────────────────────────────────

data class ProfileResponseDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("supplier") val supplier: SupplierProfileDto? = null,
    // NOTE: buyer-role key assumed to mirror "supplier" — no live sample of a buyer's
    // GET /profile response was seen. Adjust the key if the backend nests it differently.
    @SerializedName("branch") val branch: BranchProfileDto? = null
) {
    fun toDomain() = ProfileInfo(
        id = id.toIntSafe().toString(),
        name = name ?: "",
        email = email ?: "",
        phone = phone ?: "",
        role = role ?: "",
        status = status,
        supplier = supplier?.toDomain(),
        branch = branch?.toDomain()
    )
}

data class SupplierProfileDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("licence_number") val licenceNumber: String? = null,
    @SerializedName("min_order_value") val minOrderValue: String? = null,
    @SerializedName("min_order_qty") val minOrderQty: Any? = null,
    @SerializedName("zones") val zones: List<ZoneDto>? = null
) {
    fun toDomain() = SupplierProfile(
        id = id.toIntSafe().toString(),
        name = name ?: "",
        licenceNumber = licenceNumber,
        minOrderValue = minOrderValue ?: "0",
        minOrderQty = minOrderQty.toIntSafe(),
        zones = zones?.map { it.toZone() } ?: emptyList()
    )
}

data class BranchProfileDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("licence_number") val licenceNumber: String? = null
) {
    fun toDomain() = BranchProfile(
        id = id.toIntSafe().toString(),
        name = name ?: "",
        address = address ?: "",
        phone = phone ?: "",
        licenceNumber = licenceNumber
    )
}

// ── Request bodies ─────────────────────────────────────────────────────────

data class ChangePasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("password") val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String
)

data class UpdateSupplierRequest(
    @SerializedName("name") val name: String,
    @SerializedName("min_order_value") val minOrderValue: Double,
    @SerializedName("min_order_qty") val minOrderQty: Int
)

data class UpdateBranchRequest(
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("licence_number") val licenceNumber: String
)

data class DeactivateAccountRequest(
    @SerializedName("phone") val phone: String
)

data class RequestZoneUpdateRequest(
    @SerializedName("zone_ids") val zoneIds: List<Int>,
    @SerializedName("reason") val reason: String
)

// ── POST /profile/request-zone-update ────────────────────────────────────────

data class ZoneUpdateRequestDto(
    @SerializedName("request_id") val requestId: Any? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("zone_ids") val zoneIds: List<Any>? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null
) {
    fun toDomain() = ZoneUpdateRequest(
        requestId = requestId.toIntSafe().toString(),
        status = status ?: "pending",
        zoneIds = zoneIds?.map { it.toIntSafe() } ?: emptyList(),
        submittedAt = submittedAt ?: ""
    )
}
