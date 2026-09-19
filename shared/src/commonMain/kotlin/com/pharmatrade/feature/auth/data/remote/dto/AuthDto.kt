package com.pharmatrade.feature.auth.data.remote.dto

import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

// Nested data object inside "data" key
@Serializable
data class AuthData(
    @SerialName("token") val token: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("user") val user: UserDto? = null
) {
    val resolvedToken: String? get() = token ?: accessToken
}

// Login request body
@Serializable
data class LoginRequest(
    @SerialName("phone") val phone: String,
    @SerialName("password") val password: String,
    @SerialName("device_token") val deviceToken: String
)

@Serializable
data class UserDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("business_name") val businessName: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("user_type") val userType: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("licence_number") val licenceNumber: String? = null,
    @SerialName("zone_id") val zoneId: JsonElement? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("min_order_value") val minOrderValue: JsonElement? = null,
    @SerialName("min_order_qty") val minOrderQty: JsonElement? = null,
    @SerialName("zone_ids") val zoneIds: List<JsonElement>? = null
) {
    fun toUser(defaultType: UserType? = null): User {
        val resolvedRole = role ?: userType ?: type
        val ut = when (resolvedRole?.lowercase()) {
            "supplier", "seller", "agent", "drug_seller_agent" -> UserType.SELLER
            "pharmacy", "buyer", "pharmacist" -> UserType.BUYER
            "admin" -> UserType.ADMIN
            else -> defaultType ?: UserType.BUYER
        }
        val userId = id.rawString() ?: ""
        return User(
            id = userId,
            name = name ?: "",
            email = email ?: "",
            phone = phone ?: "",
            userType = ut,
            status = status,
            businessName = businessName ?: "",
            sellerId = if (ut == UserType.SELLER) userId else null,
            licenceNumber = licenceNumber,
            zoneId = zoneId.rawString(),
            address = address,
            additionalZoneIds = zoneIds?.map { it.rawString() ?: "" } ?: emptyList(),
            minOrderValue = minOrderValue.rawString(),
            minOrderQty = minOrderQty.rawString()
        )
    }
}

// The backend sends some numeric-ish fields (ids, zone ids, order minimums) as either
// JSON numbers or strings depending on endpoint — read either shape as plain text.
private fun JsonElement?.rawString(): String? =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
