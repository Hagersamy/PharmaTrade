package com.pharmatrade.feature.admin.data.remote

import com.pharmatrade.feature.admin.data.remote.dto.PendingUserDto
import com.pharmatrade.feature.admin.data.remote.dto.PendingUsersPageDto
import com.pharmatrade.feature.admin.data.remote.dto.RegistrationStatsDto
import com.pharmatrade.feature.auth.data.remote.dto.ApiResponse
import okhttp3.RequestBody
import retrofit2.http.*

interface AdminApiService {

    @GET("admin/pharmacies")
    suspend fun getPendingPharmacies(
        @Query("status") status: String = "pending"
    ): ApiResponse<List<PendingUserDto>>

    @GET("admin/suppliers")
    suspend fun getPendingSuppliers(
        @Query("status") status: String = "pending"
    ): ApiResponse<List<PendingUserDto>>

    @POST("admin/pharmacies/{id}/approve")
    suspend fun approvePharmacy(@Path("id") id: String): ApiResponse<Any>

    @POST("admin/pharmacies/{id}/reject")
    suspend fun rejectPharmacy(@Path("id") id: String): ApiResponse<Any>

    @POST("admin/suppliers/{id}/approve")
    suspend fun approveSupplier(@Path("id") id: String): ApiResponse<Any>

    @POST("admin/suppliers/{id}/reject")
    suspend fun rejectSupplier(@Path("id") id: String): ApiResponse<Any>

    // ── Unified registration-requests API ─────────────────────────────────────

    @GET("admin/registration-requests/stats")
    suspend fun getRegistrationStats(): ApiResponse<RegistrationStatsDto>

    @GET("admin/registration-requests")
    suspend fun getRegistrationRequests(
        @Query("status") status: String = "pending",
        @Query("entity_type") entityType: String? = null
    ): ApiResponse<PendingUsersPageDto>

    @Multipart
    @POST("admin/registration-requests/{id}/approve")
    suspend fun approveRequest(
        @Path("id") id: String,
        @Part("notes") notes: RequestBody
    ): ApiResponse<Any>

    @Multipart
    @POST("admin/registration-requests/{id}/decline")
    suspend fun declineRequest(
        @Path("id") id: String,
        @Part("decline_reason") reason: RequestBody
    ): ApiResponse<Any>
}
