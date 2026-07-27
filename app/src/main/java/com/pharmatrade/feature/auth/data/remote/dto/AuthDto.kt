package com.pharmatrade.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType

// Generic API wrapper used for all responses
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("errors") val errors: Map<String, List<String>>? = null,
    // Flat format: some APIs return token/user at root level instead of inside "data"
    @SerializedName("token") val token: String? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("user") val userRaw: UserDto? = null
) {
    val isSuccessful: Boolean
        get() = success == true || status == true ||
                data != null || userRaw != null || token != null
    val errorMessage: String?
        get() = errors?.values?.firstOrNull()?.firstOrNull() ?: message
}

// Nested data object inside "data" key
data class AuthData(
    @SerializedName("token") val token: String? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("user") val user: UserDto? = null
) {
    val resolvedToken: String? get() = token ?: accessToken
}

// Login request body
data class LoginRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

data class UserDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("business_name") val businessName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("user_type") val userType: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("licence_number") val licenceNumber: String? = null,
    @SerializedName("zone_id") val zoneId: Any? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("min_order_value") val minOrderValue: Any? = null,
    @SerializedName("min_order_qty") val minOrderQty: Any? = null,
    @SerializedName("zone_ids") val zoneIds: List<Any>? = null
) {
    fun toUser(defaultType: UserType? = null): User {
        val resolvedRole = role ?: userType ?: type
        val ut = when (resolvedRole?.lowercase()) {
            "supplier", "seller", "agent", "drug_seller_agent" -> UserType.SELLER
            "pharmacy", "buyer", "pharmacist" -> UserType.BUYER
            "admin" -> UserType.ADMIN
            else -> defaultType ?: UserType.BUYER
        }
        val userId = id?.toString() ?: ""
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
            zoneId = zoneId?.toString(),
            address = address,
            additionalZoneIds = zoneIds?.map { it.toString() } ?: emptyList(),
            minOrderValue = minOrderValue?.toString(),
            minOrderQty = minOrderQty?.toString()
        )
    }
}
