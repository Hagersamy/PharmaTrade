package com.pharmatrade.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// kotlinx.serialization counterpart of the Gson-based ApiResponse still used by
// features not yet migrated off Retrofit (com.pharmatrade.feature.auth.data.remote.dto.ApiResponse).
@Serializable
data class ApiResponse<T>(
    @SerialName("success") val success: Boolean? = null,
    @SerialName("status") val status: Boolean? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: T? = null,
    @SerialName("errors") val errors: Map<String, List<String>>? = null,
    @SerialName("token") val token: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    // Kept raw so this core module stays feature-agnostic; auth decodes it (see AuthDto.topLevelUser).
    @SerialName("user") val userRaw: JsonElement? = null
) {
    val isSuccessful: Boolean
        get() = success == true || status == true ||
                data != null || userRaw != null || token != null

    val errorMessage: String?
        get() = errors?.values?.firstOrNull()?.firstOrNull() ?: message
}
