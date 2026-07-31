package com.pharmatrade.feature.pharmacyorder.data.remote

import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.FormFile
import com.pharmatrade.core.network.dto.ApiResponse
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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class PharmacyOrderApi(private val client: HttpClient = ApiClient.httpClient) {

    suspend fun getBranch(): ApiResponse<BranchDto> =
        client.get("pharmacy/branch").body()

    suspend fun getOrders(status: String? = null, perPage: Int? = null): ApiResponse<OrderPageDto> =
        client.get("pharmacy/orders") {
            parameter("status", status)
            parameter("per_page", perPage)
        }.body()

    suspend fun getSuppliers(): ApiResponse<List<SupplierDto>> =
        client.get("pharmacy/suppliers").body()

    suspend fun getSupplierInventory(supplierId: String, page: Int? = null): ApiResponse<SupplierInventoryResponseDto> =
        client.get("pharmacy/suppliers/$supplierId/inventory") { parameter("page", page) }.body()

    suspend fun getAllSuppliersDrugs(page: Int? = null): ApiResponse<SupplierCatalogPageDto> =
        client.get("pharmacy/suppliers/drugs") { parameter("page", page) }.body()

    suspend fun createOrder(request: CreateOrderRequest): ApiResponse<OrderDetailDto> =
        client.post("pharmacy/orders") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun addItem(orderId: String, request: AddItemRequest): ApiResponse<OrderItemDto> =
        client.post("pharmacy/orders/$orderId/items") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun removeItem(orderId: String, itemId: String) {
        client.delete("pharmacy/orders/$orderId/items/$itemId")
    }

    suspend fun uploadItems(orderId: String, file: FormFile): ApiResponse<UploadItemsDto> =
        client.submitFormWithBinaryData(
            url = "pharmacy/orders/$orderId/upload",
            formData = formData {
                append(
                    "file", file.bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, file.mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
                    }
                )
            }
        ).body()

    suspend fun allocateOrder(orderId: String, request: AllocateRequest): ApiResponse<OrderDetailDto> =
        client.post("pharmacy/orders/$orderId/allocate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getOrderDetail(orderId: String): ApiResponse<OrderDetailDto> =
        client.get("pharmacy/orders/$orderId").body()

    suspend fun resolveShortage(orderId: String, request: ResolveShortageRequest) {
        client.post("pharmacy/orders/$orderId/resolve-shortage") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun cancelOrder(orderId: String) {
        client.patch("pharmacy/orders/$orderId/cancel")
    }

    // Same body shape as resolve-shortage — for a plain "mark as delivered" (no shortage
    // involved) the caller sends action="confirm" with an empty shortage_report_ids.
    suspend fun confirmDelivery(orderId: String, request: ResolveShortageRequest) {
        client.post("pharmacy/orders/$orderId/confirm-delivery") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
