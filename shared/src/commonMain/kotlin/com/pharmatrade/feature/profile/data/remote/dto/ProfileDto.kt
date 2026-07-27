package com.pharmatrade.feature.profile.data.remote.dto

import com.pharmatrade.core.network.dto.rawIntOrZero
import com.pharmatrade.feature.auth.data.remote.dto.ZoneDto
import com.pharmatrade.feature.profile.domain.model.BranchProfile
import com.pharmatrade.feature.profile.domain.model.ProfileInfo
import com.pharmatrade.feature.profile.domain.model.SupplierProfile
import com.pharmatrade.feature.profile.domain.model.ZoneUpdateRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── GET /profile ───────────────────────────────────────────────────────────

@Serializable
data class ProfileResponseDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("supplier") val supplier: SupplierProfileDto? = null,
    // NOTE: buyer-role key assumed to mirror "supplier" — no live sample of a buyer's
    // GET /profile response was seen. Adjust the key if the backend nests it differently.
    @SerialName("branch") val branch: BranchProfileDto? = null
) {
    fun toDomain() = ProfileInfo(
        id = id.rawIntOrZero().toString(),
        name = name ?: "",
        email = email ?: "",
        phone = phone ?: "",
        role = role ?: "",
        status = status,
        supplier = supplier?.toDomain(),
        branch = branch?.toDomain()
    )
}

@Serializable
data class SupplierProfileDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("licence_number") val licenceNumber: String? = null,
    @SerialName("min_order_value") val minOrderValue: String? = null,
    @SerialName("min_order_qty") val minOrderQty: JsonElement? = null,
    @SerialName("zones") val zones: List<ZoneDto>? = null
) {
    fun toDomain() = SupplierProfile(
        id = id.rawIntOrZero().toString(),
        name = name ?: "",
        licenceNumber = licenceNumber,
        minOrderValue = minOrderValue ?: "0",
        minOrderQty = minOrderQty.rawIntOrZero(),
        zones = zones?.map { it.toZone() } ?: emptyList()
    )
}

@Serializable
data class BranchProfileDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("licence_number") val licenceNumber: String? = null
) {
    fun toDomain() = BranchProfile(
        id = id.rawIntOrZero().toString(),
        name = name ?: "",
        address = address ?: "",
        phone = phone ?: "",
        licenceNumber = licenceNumber
    )
}

// ── Request bodies ─────────────────────────────────────────────────────────

@Serializable
data class ChangePasswordRequest(
    @SerialName("email") val email: String,
    @SerialName("current_password") val currentPassword: String,
    @SerialName("password") val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String
)

@Serializable
data class UpdateSupplierRequest(
    @SerialName("name") val name: String,
    @SerialName("min_order_value") val minOrderValue: Double,
    @SerialName("min_order_qty") val minOrderQty: Int
)

@Serializable
data class UpdateBranchRequest(
    @SerialName("name") val name: String,
    @SerialName("address") val address: String,
    @SerialName("phone") val phone: String,
    @SerialName("licence_number") val licenceNumber: String
)

@Serializable
data class DeactivateAccountRequest(
    @SerialName("phone") val phone: String
)

@Serializable
data class RequestZoneUpdateRequest(
    @SerialName("zone_ids") val zoneIds: List<Int>,
    @SerialName("reason") val reason: String
)

// ── POST /profile/request-zone-update ────────────────────────────────────────

@Serializable
data class ZoneUpdateRequestDto(
    @SerialName("request_id") val requestId: JsonElement? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("zone_ids") val zoneIds: List<JsonElement>? = null,
    @SerialName("submitted_at") val submittedAt: String? = null
) {
    fun toDomain() = ZoneUpdateRequest(
        requestId = requestId.rawIntOrZero().toString(),
        status = status ?: "pending",
        zoneIds = zoneIds?.map { it.rawIntOrZero() } ?: emptyList(),
        submittedAt = submittedAt ?: ""
    )
}
