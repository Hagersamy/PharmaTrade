package com.pharmatrade.feature.marketplace.data.remote

import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.FormFile
import com.pharmatrade.feature.marketplace.data.remote.dto.AddInventoryRequest
import com.pharmatrade.feature.marketplace.data.remote.dto.CollectionItemsRequest
import com.pharmatrade.feature.marketplace.data.remote.dto.CreateCollectionRequest
import com.pharmatrade.feature.marketplace.data.remote.dto.FeaturedItemsRequest
import com.pharmatrade.feature.marketplace.data.remote.dto.PlaceOrderRequest
import com.pharmatrade.feature.marketplace.data.remote.dto.SetCollectorRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement

// Every call returns the raw JSON: the backend's Postman suite reads these responses unwrapped
// (`json.id`, `json.data[]`) while the rest of the API wraps them in {success, data}, so the
// repository unwraps either shape (see MarketplaceRepositoryImpl.unwrapObject/unwrapList).
class MarketplaceApi(private val client: HttpClient = ApiClient.httpClient) {

    // ── Inventory (seller) ──

    suspend fun getInventory(perPage: Int): JsonElement =
        client.get("$BASE/inventory") { parameter("per_page", perPage) }.body()

    suspend fun addInventory(request: AddInventoryRequest): JsonElement =
        client.post("$BASE/inventory") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // NOTE: assumed — not in the backend docs; mirrors the supplier's PUT supplier/inventory/{id}.
    // The repository falls back to the (upserting) POST if this route doesn't exist.
    suspend fun updateInventory(id: String, request: AddInventoryRequest): JsonElement =
        client.put("$BASE/inventory/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun importInventory(file: FormFile): JsonElement =
        client.submitFormWithBinaryData(
            url = "$BASE/inventory/import",
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

    // ── Collections (seller) ──

    suspend fun getMyCollections(perPage: Int): JsonElement =
        client.get("$BASE/my-collections") { parameter("per_page", perPage) }.body()

    suspend fun getMyCollection(id: String): JsonElement =
        client.get("$BASE/my-collections/$id").body()

    suspend fun createCollection(request: CreateCollectionRequest): JsonElement =
        client.post("$BASE/my-collections") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun setCollectionItems(id: String, request: CollectionItemsRequest): JsonElement =
        client.post("$BASE/my-collections/$id/items") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // The backend note says collections/{id}/featured-items, but the server only routes it under
    // my-collections like the other seller endpoints (checked 2026-10-16).
    suspend fun setFeaturedItems(id: String, request: FeaturedItemsRequest): JsonElement =
        client.post("$BASE/my-collections/$id/featured-items") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun publishCollection(id: String): JsonElement =
        client.post("$BASE/my-collections/$id/publish").body()

    suspend fun deactivateCollection(id: String): JsonElement =
        client.post("$BASE/my-collections/$id/deactivate").body()

    // ── Incoming orders (seller) ──

    suspend fun getSales(perPage: Int): JsonElement =
        client.get("$BASE/seller/orders") { parameter("per_page", perPage) }.body()

    // NOTE: assumed — not in the backend docs, which only list the seller/orders index.
    suspend fun getSale(orderId: String): JsonElement =
        client.get("$BASE/seller/orders/$orderId").body()

    suspend fun sellerOrderAction(orderId: String, action: String): JsonElement =
        client.post("$BASE/seller/orders/$orderId/$action").body()

    // ── Buying ──

    suspend fun browse(page: Int): JsonElement =
        client.get("$BASE/browse") { parameter("page", page) }.body()

    suspend fun getBrowseCollection(id: String): JsonElement =
        client.get("$BASE/browse/$id").body()

    suspend fun placeOrder(request: PlaceOrderRequest): JsonElement =
        client.post("$BASE/orders") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // The flow doc says .../collector but the backend's Postman suite (the tested source)
    // uses .../set-collector.
    suspend fun setCollector(orderId: String, request: SetCollectorRequest): JsonElement =
        client.post("$BASE/orders/$orderId/set-collector") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun cancelPurchase(orderId: String) {
        client.post("$BASE/orders/$orderId/cancel")
    }

    // NOTE: assumed — the docs have POST orders (place) but no buyer-side order list/detail;
    // these follow the same REST shape as seller/orders.
    suspend fun getPurchases(perPage: Int): JsonElement =
        client.get("$BASE/orders") { parameter("per_page", perPage) }.body()

    suspend fun getPurchase(orderId: String): JsonElement =
        client.get("$BASE/orders/$orderId").body()

    private companion object {
        const val BASE = "pharmacy/marketplace"
    }
}
