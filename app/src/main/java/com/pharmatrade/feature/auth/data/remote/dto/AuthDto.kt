package com.pharmatrade.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

// Generic API wrapper used by features still on Retrofit/Gson (admin, drugs, pharmacyorder,
// profile, supplierorder). Auth itself has moved to the kotlinx.serialization equivalent at
// com.pharmatrade.core.network.dto.ApiResponse — this one stays only for the callers above.
// UserDto is resolved from :shared (com.pharmatrade.feature.auth.data.remote.dto.UserDto);
// none of the remaining Gson callers read the `userRaw` field, so its kotlinx.serialization-only
// annotations don't affect them.
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
