package com.pharmatrade.feature.supplierorder.data.remote

import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.dto.ApiResponse
import com.pharmatrade.feature.supplierorder.data.remote.dto.ConfirmOrderRequest
import com.pharmatrade.feature.supplierorder.data.remote.dto.ReportShortageRequest
import com.pharmatrade.feature.supplierorder.data.remote.dto.SupplierOrderDetailDto
import com.pharmatrade.feature.supplierorder.data.remote.dto.SupplierOrderPageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class SupplierOrderApi(private val client: HttpClient = ApiClient.httpClient) {

    suspend fun getOrders(status: String? = null, perPage: Int? = null): ApiResponse<SupplierOrderPageDto> =
        client.get("supplier/orders") {
            parameter("status", status)
            parameter("per_page", perPage)
        }.body()

    suspend fun getOrderDetail(orderId: String): ApiResponse<SupplierOrderDetailDto> =
        client.get("supplier/orders/$orderId").body()

    suspend fun confirmOrder(orderId: String, request: ConfirmOrderRequest) {
        client.post("supplier/orders/$orderId/confirm") { setBody(request) }
    }

    suspend fun reportShortage(orderId: String, request: ReportShortageRequest) {
        client.post("supplier/orders/$orderId/report-shortage") { setBody(request) }
    }

    suspend fun shipOrder(orderId: String) {
        client.post("supplier/orders/$orderId/ship")
    }

    suspend fun deliverOrder(orderId: String) {
        client.patch("supplier/orders/$orderId/deliver")
    }
}
