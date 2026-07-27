package com.pharmatrade.feature.supplierorder.data.remote

import com.pharmatrade.feature.auth.data.remote.dto.ApiResponse
import com.pharmatrade.feature.supplierorder.data.remote.dto.ConfirmOrderRequest
import com.pharmatrade.feature.supplierorder.data.remote.dto.ReportShortageRequest
import com.pharmatrade.feature.supplierorder.data.remote.dto.SupplierOrderDetailDto
import com.pharmatrade.feature.supplierorder.data.remote.dto.SupplierOrderPageDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SupplierOrderApiService {

    @GET("supplier/orders")
    suspend fun getOrders(
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int? = null
    ): ApiResponse<SupplierOrderPageDto>

    @GET("supplier/orders/{id}")
    suspend fun getOrderDetail(@Path("id") orderId: String): ApiResponse<SupplierOrderDetailDto>

    @POST("supplier/orders/{id}/confirm")
    suspend fun confirmOrder(
        @Path("id") orderId: String,
        @Body request: ConfirmOrderRequest
    ): ApiResponse<Any?>

    @POST("supplier/orders/{id}/report-shortage")
    suspend fun reportShortage(
        @Path("id") orderId: String,
        @Body request: ReportShortageRequest
    ): ApiResponse<Any?>

    @POST("supplier/orders/{id}/ship")
    suspend fun shipOrder(@Path("id") orderId: String): ApiResponse<Any?>

    @PATCH("supplier/orders/{id}/deliver")
    suspend fun deliverOrder(@Path("id") orderId: String): ApiResponse<Any?>
}
