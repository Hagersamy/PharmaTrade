package com.pharmatrade.feature.profile.data.remote

import com.pharmatrade.feature.auth.data.remote.dto.ApiResponse
import com.pharmatrade.feature.profile.data.remote.dto.BranchProfileDto
import com.pharmatrade.feature.profile.data.remote.dto.ChangePasswordRequest
import com.pharmatrade.feature.profile.data.remote.dto.DeactivateAccountRequest
import com.pharmatrade.feature.profile.data.remote.dto.ProfileResponseDto
import com.pharmatrade.feature.profile.data.remote.dto.RequestZoneUpdateRequest
import com.pharmatrade.feature.profile.data.remote.dto.SupplierProfileDto
import com.pharmatrade.feature.profile.data.remote.dto.UpdateBranchRequest
import com.pharmatrade.feature.profile.data.remote.dto.UpdateSupplierRequest
import com.pharmatrade.feature.profile.data.remote.dto.ZoneUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

interface ProfileApiService {

    @GET("profile")
    suspend fun getProfile(): ApiResponse<ProfileResponseDto>

    // PUT /profile is a partial update — only the fields the user actually changed are sent
    // (plus "password", always required to confirm the change), so a Map is used instead of a
    // fixed DTO with required properties.
    @PUT("profile")
    suspend fun updateProfile(@Body request: Map<String, @JvmSuppressWildcards Any>): ApiResponse<ProfileResponseDto>

    @PUT("profile/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ApiResponse<Unit>

    @PATCH("profile/supplier")
    suspend fun updateSupplier(@Body request: UpdateSupplierRequest): ApiResponse<SupplierProfileDto>

    @PATCH("profile/branch")
    suspend fun updateBranch(@Body request: UpdateBranchRequest): ApiResponse<BranchProfileDto>

    // Retrofit's @DELETE doesn't support @Body directly — @HTTP with hasBody = true is the
    // documented way to send a DELETE request with a JSON body.
    @HTTP(method = "DELETE", path = "profile", hasBody = true)
    suspend fun deactivateAccount(@Body request: DeactivateAccountRequest): ApiResponse<Unit>

    // Available to both pharmacy (branch) and supplier accounts — submits a pending request an
    // admin must approve rather than updating the zones directly.
    @POST("profile/request-zone-update")
    suspend fun requestZoneUpdate(@Body request: RequestZoneUpdateRequest): ApiResponse<ZoneUpdateRequestDto>
}
