package com.pharmatrade.feature.drugs.data.remote

import com.pharmatrade.feature.auth.data.remote.dto.ApiResponse
import com.pharmatrade.feature.drugs.data.remote.dto.CreateDrugRequest
import com.pharmatrade.feature.drugs.data.remote.dto.DrugDto
import com.pharmatrade.feature.drugs.data.remote.dto.DrugPageDto
import com.pharmatrade.feature.drugs.data.remote.dto.InventoryItemDto
import com.pharmatrade.feature.drugs.data.remote.dto.InventoryItemRequest
import com.pharmatrade.feature.drugs.data.remote.dto.InventoryResponseDto
import com.pharmatrade.feature.drugs.data.remote.dto.UploadHistoryDto
import okhttp3.MultipartBody
import retrofit2.http.*

interface DrugApiService {

    @GET("drugs")
    suspend fun getDrugs(
        @Query("search") search: String? = null,
        @Query("dosage_form") dosageForm: String? = null,
        @Query("per_page") perPage: Int? = null
    ): ApiResponse<DrugPageDto>

    @GET("drugs/{id}")
    suspend fun getDrugById(@Path("id") id: String): ApiResponse<DrugDto>

    @GET("drugs/dosage-forms")
    suspend fun getDosageForms(): ApiResponse<List<String>>

    @POST("admin/drugs")
    suspend fun createDrug(@Body request: CreateDrugRequest): ApiResponse<DrugDto>

    // Lets a logged-in supplier register a single new drug into the catalog (as opposed to
    // admin/drugs, and separate from the bulk supplier/inventory/upload flow).
    @POST("supplier/drugs")
    suspend fun createSupplierDrug(@Body request: CreateDrugRequest): ApiResponse<DrugDto>

    @Multipart
    @POST("supplier/inventory/upload")
    suspend fun uploadInventory(@Part file: MultipartBody.Part): ApiResponse<UploadHistoryDto>

    @GET("supplier/inventory/upload-history")
    suspend fun getUploadHistory(): ApiResponse<List<UploadHistoryDto>>

    // per_page is deliberately not exposed here — sending it was found to change what the
    // backend scopes results to (see DrugRepositoryImpl.getInventory). Page through with the
    // backend's own default page size only.
    @GET("supplier/inventory")
    suspend fun getInventory(@Query("page") page: Int? = null): ApiResponse<InventoryResponseDto>

    // Adds a drug to the supplier's own inventory (the "Add Drug Listing" flow) —
    // takes a free-text drug name and lets the backend fuzzy-match it against the catalog.
    @POST("supplier/inventory")
    suspend fun createInventoryItem(@Body request: InventoryItemRequest): ApiResponse<InventoryItemDto>

    // NOTE: assumed PUT to match Laravel's conventional resource "update" route — if the
    // backend actually expects PATCH instead, swap the annotation, the rest is unaffected.
    @PUT("supplier/inventory/{id}")
    suspend fun updateInventoryItem(
        @Path("id") id: String,
        @Body request: InventoryItemRequest
    ): ApiResponse<InventoryItemDto>
}
