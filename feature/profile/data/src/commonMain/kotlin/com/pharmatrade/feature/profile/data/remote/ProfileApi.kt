package com.pharmatrade.feature.profile.data.remote

import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.FormFile
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
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement

class ProfileApi(private val client: HttpClient = ApiClient.httpClient) {

    // Raw JSON so the repository can look for the supplier logo wherever the backend puts it.
    suspend fun getProfile(): JsonElement =
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

    // POST supplier/profile/logo (multipart) — replaces the supplier's logo. Read as raw JSON, not
    // ApiResponse: the reply is {"message": ..., "logo_url": ...} with no success/data key, which
    // ApiResponse.isSuccessful would count as a failure. Non-2xx still throws ResponseException.
    suspend fun uploadLogo(logo: FormFile): JsonElement = client.submitFormWithBinaryData(
        url = "supplier/profile/logo",
        formData = formData {
            append("logo", logo.bytes, Headers.build {
                append(HttpHeaders.ContentType, logo.mimeType)
                append(HttpHeaders.ContentDisposition, "filename=\"${logo.fileName}\"")
            })
        }
    ).body()

    suspend fun requestZoneUpdate(request: RequestZoneUpdateRequest): ApiResponse<ZoneUpdateRequestDto> =
        client.post("profile/request-zone-update") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
