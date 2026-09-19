package com.pharmatrade.feature.profile.data.remote

import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.dto.ApiResponse
import com.pharmatrade.feature.profile.data.remote.dto.BranchProfileDto
import com.pharmatrade.feature.profile.data.remote.dto.ChangePasswordRequest
import com.pharmatrade.feature.profile.data.remote.dto.ProfileResponseDto
import com.pharmatrade.feature.profile.data.remote.dto.RequestZoneUpdateRequest
import com.pharmatrade.feature.profile.data.remote.dto.SupplierProfileDto
import com.pharmatrade.feature.profile.data.remote.dto.UpdateBranchRequest
import com.pharmatrade.feature.profile.data.remote.dto.UpdateSupplierRequest
import com.pharmatrade.feature.profile.data.remote.dto.ZoneUpdateRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement

class ProfileApi(private val client: HttpClient = ApiClient.httpClient) {

    suspend fun getProfile(): ApiResponse<ProfileResponseDto> =
        client.get("profile").body()

    // PUT /profile is a partial update — only the fields the user actually changed are sent
    // (plus "password", always required to confirm the change), so a plain string map is used
    // instead of a fixed DTO with required properties.
    suspend fun updateProfile(request: Map<String, String>): ApiResponse<ProfileResponseDto> =
        client.put("profile") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun changePassword(request: ChangePasswordRequest): ApiResponse<JsonElement?> =
        client.patch("profile/password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun updateSupplier(request: UpdateSupplierRequest): ApiResponse<SupplierProfileDto> =
        client.patch("profile/supplier") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun updateBranch(request: UpdateBranchRequest): ApiResponse<BranchProfileDto> =
        client.patch("profile/branch") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun requestZoneUpdate(request: RequestZoneUpdateRequest): ApiResponse<ZoneUpdateRequestDto> =
        client.post("profile/request-zone-update") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
