package com.pharmatrade.feature.pharmacyorder.data.remote

import com.pharmatrade.feature.auth.data.remote.dto.ApiResponse
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.AddItemRequest
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.AllocateRequest
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.BranchDto
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.CreateOrderRequest
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.OrderDetailDto
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.OrderItemDto
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.OrderPageDto
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.ResolveShortageRequest
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.SupplierCatalogPageDto
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.SupplierDto
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.SupplierInventoryResponseDto
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.UploadItemsDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PharmacyOrderApiService {

    @GET("pharmacy/branch")
    suspend fun getBranch(): ApiResponse<BranchDto>

    @GET("pharmacy/orders")
    suspend fun getOrders(
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int? = null
    ): ApiResponse<OrderPageDto>

    @GET("pharmacy/suppliers")
    suspend fun getSuppliers(): ApiResponse<List<SupplierDto>>

    @GET("pharmacy/suppliers/{id}/inventory")
    suspend fun getSupplierInventory(
        @Path("id") supplierId: String,
        @Query("page") page: Int? = null
    ): ApiResponse<SupplierInventoryResponseDto>

    @GET("pharmacy/suppliers/drugs")
    suspend fun getAllSuppliersDrugs(@Query("page") page: Int? = null): ApiResponse<SupplierCatalogPageDto>

    @POST("pharmacy/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): ApiResponse<OrderDetailDto>

    @POST("pharmacy/orders/{id}/items")
    suspend fun addItem(
        @Path("id") orderId: String,
        @Body request: AddItemRequest
    ): ApiResponse<OrderItemDto>

    @DELETE("pharmacy/orders/{id}/items/{itemId}")
    suspend fun removeItem(
        @Path("id") orderId: String,
        @Path("itemId") itemId: String
    ): ApiResponse<Any?>

    @Multipart
    @POST("pharmacy/orders/{id}/upload")
    suspend fun uploadItems(
        @Path("id") orderId: String,
        @Part file: MultipartBody.Part
    ): ApiResponse<UploadItemsDto>

    @POST("pharmacy/orders/{id}/allocate")
    suspend fun allocateOrder(
        @Path("id") orderId: String,
        @Body request: AllocateRequest
    ): ApiResponse<OrderDetailDto>

    @GET("pharmacy/orders/{id}")
    suspend fun getOrderDetail(@Path("id") orderId: String): ApiResponse<OrderDetailDto>

    @POST("pharmacy/orders/{id}/resolve-shortage")
    suspend fun resolveShortage(
        @Path("id") orderId: String,
        @Body request: ResolveShortageRequest
    ): ApiResponse<Any?>

    @PATCH("pharmacy/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") orderId: String): ApiResponse<Any?>

    // Same body shape as resolve-shortage — for a plain "mark as delivered" (no shortage
    // involved) the caller sends action="confirm" with an empty shortage_report_ids.
    @POST("pharmacy/orders/{id}/confirm-delivery")
    suspend fun confirmDelivery(
        @Path("id") orderId: String,
        @Body request: ResolveShortageRequest
    ): ApiResponse<Any?>
}
