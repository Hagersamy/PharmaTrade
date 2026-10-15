package com.pharmatrade.feature.marketplace.data.repository

import com.pharmatrade.core.common.error.UserFacingException
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.io.PlatformFileReader
import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.FormFile
import com.pharmatrade.feature.marketplace.data.remote.MarketplaceApi
import com.pharmatrade.feature.marketplace.data.remote.dto.AddInventoryRequest
import com.pharmatrade.feature.marketplace.data.remote.dto.CollectionItemRequest
import com.pharmatrade.feature.marketplace.data.remote.dto.CollectionItemsRequest
import com.pharmatrade.feature.marketplace.data.remote.dto.CreateCollectionRequest
import com.pharmatrade.feature.marketplace.data.remote.dto.MarketCollectionDto
import com.pharmatrade.feature.marketplace.data.remote.dto.MarketInventoryDto
import com.pharmatrade.feature.marketplace.data.remote.dto.MarketOrderDto
import com.pharmatrade.feature.marketplace.data.remote.dto.PlaceOrderRequest
import com.pharmatrade.feature.marketplace.data.remote.dto.SetCollectorRequest
import com.pharmatrade.feature.marketplace.domain.model.CollectionItemInput
import com.pharmatrade.feature.marketplace.domain.model.MarketCollection
import com.pharmatrade.feature.marketplace.domain.model.MarketInventoryItem
import com.pharmatrade.feature.marketplace.domain.model.MarketOrder
import com.pharmatrade.feature.marketplace.domain.model.MarketPage
import com.pharmatrade.feature.marketplace.domain.model.SellerOrderAction
import com.pharmatrade.feature.marketplace.domain.repository.MarketplaceRepository
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MarketplaceRepositoryImpl(
    private val fileReader: PlatformFileReader,
    private val api: MarketplaceApi = MarketplaceApi()
) : MarketplaceRepository {

    // ── Selling: inventory ──

    override suspend fun getInventory(): Result<List<MarketInventoryItem>> = call("Failed to load inventory") {
        decodeList(api.getInventory(LIST_PAGE_SIZE), MarketInventoryDto.serializer()).map { it.toDomain() }
    }

    override suspend fun addInventoryItem(
        drugId: String,
        quantity: Int,
        unitPrice: Double,
        expiryDate: String,
        batchNumber: String?
    ): Result<MarketInventoryItem> = call("Failed to add drug") {
        val request = AddInventoryRequest(
            drugId = drugId.toInt(),
            quantityAvailable = quantity,
            unitPrice = unitPrice,
            expiryDate = expiryDate,
            batchNumber = batchNumber
        )
        decodeObject(api.addInventory(request), MarketInventoryDto.serializer()).toDomain()
    }

    override suspend fun updateInventoryItem(
        inventoryId: String,
        drugId: String,
        quantity: Int,
        unitPrice: Double,
        expiryDate: String,
        batchNumber: String?
    ): Result<MarketInventoryItem> = call("Failed to update drug") {
        val request = AddInventoryRequest(
            drugId = drugId.toInt(),
            quantityAvailable = quantity,
            unitPrice = unitPrice,
            expiryDate = expiryDate,
            batchNumber = batchNumber
        )
        val body = try {
            api.updateInventory(inventoryId, request)
        } catch (e: ResponseException) {
            // PUT isn't documented; POST inventory upserts per drug, so it updates the same row.
            if (e.response.status.value !in NOT_ROUTED) throw e
            api.addInventory(request)
        }
        decodeObject(body, MarketInventoryDto.serializer()).toDomain()
    }

    override suspend fun importInventory(fileUri: String, fileName: String): Result<Unit> = call("Upload failed") {
        val info = fileReader.read(fileUri) ?: return Result.Error("Could not read the selected file.")
        val resolvedName = info.displayName.ifBlank { fileName }
        val mime = when (resolvedName.substringAfterLast('.', "").lowercase()) {
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "csv" -> "text/csv"
            else -> info.mimeType ?: "application/octet-stream"
        }
        // The import runs as a queued job server-side, so there's nothing to read back yet.
        api.importInventory(FormFile(bytes = info.bytes, fileName = resolvedName, mimeType = mime))
        Unit
    }

    // ── Selling: collections ──

    override suspend fun getMyCollections(): Result<List<MarketCollection>> = call("Failed to load collections") {
        decodeList(api.getMyCollections(LIST_PAGE_SIZE), MarketCollectionDto.serializer()).map { it.toDomain() }
    }

    override suspend fun getMyCollection(collectionId: String): Result<MarketCollection> = call("Failed to load collection") {
        decodeObject(api.getMyCollection(collectionId), MarketCollectionDto.serializer()).toDomain()
    }

    override suspend fun createCollection(
        name: String,
        description: String?,
        notes: String?
    ): Result<MarketCollection> = call("Failed to create collection") {
        val request = CreateCollectionRequest(name = name, description = description, notes = notes)
        decodeObject(api.createCollection(request), MarketCollectionDto.serializer()).toDomain()
    }

    override suspend fun setCollectionItems(
        collectionId: String,
        items: List<CollectionItemInput>
    ): Result<MarketCollection> = call("Failed to save items") {
        val request = CollectionItemsRequest(
            items.map { CollectionItemRequest(inventoryId = it.inventoryId.toInt(), quantity = it.quantity) }
        )
        decodeObject(api.setCollectionItems(collectionId, request), MarketCollectionDto.serializer()).toDomain()
    }

    override suspend fun publishCollection(collectionId: String): Result<MarketCollection> = call("Failed to publish") {
        decodeObject(api.publishCollection(collectionId), MarketCollectionDto.serializer()).toDomain()
    }

    override suspend fun deactivateCollection(collectionId: String): Result<MarketCollection> = call("Failed to deactivate") {
        decodeObject(api.deactivateCollection(collectionId), MarketCollectionDto.serializer()).toDomain()
    }

    // ── Selling: incoming orders ──

    override suspend fun getSales(): Result<List<MarketOrder>> = call("Failed to load orders") {
        decodeList(api.getSales(LIST_PAGE_SIZE), MarketOrderDto.serializer()).map { it.toDomain() }
    }

    override suspend fun sellerOrderAction(orderId: String, action: SellerOrderAction): Result<MarketOrder> =
        call("Failed to update order") {
            decodeObject(api.sellerOrderAction(orderId, action.path), MarketOrderDto.serializer()).toDomain()
        }

    // ── Buying ──

    override suspend fun browse(page: Int): Result<MarketPage<MarketCollection>> = call("Failed to load collections") {
        val body = api.browse(page)
        val items = decodeList(body, MarketCollectionDto.serializer()).map { it.toDomain() }
        val meta = paginatorOf(body)
        val currentPage = meta?.intField("current_page") ?: page
        MarketPage(items = items, currentPage = currentPage, lastPage = meta?.intField("last_page") ?: currentPage)
    }

    override suspend fun getBrowseCollection(collectionId: String): Result<MarketCollection> = call("Failed to load collection") {
        decodeObject(api.getBrowseCollection(collectionId), MarketCollectionDto.serializer()).toDomain()
    }

    override suspend fun placeOrder(collectionId: String): Result<MarketOrder> = call("Failed to place order") {
        decodeObject(api.placeOrder(PlaceOrderRequest(collectionId.toInt())), MarketOrderDto.serializer()).toDomain()
    }

    override suspend fun setCollector(orderId: String, name: String, phone: String): Result<MarketOrder> =
        call("Failed to save collector") {
            val request = SetCollectorRequest(collectorName = name, collectorPhone = phone)
            decodeObject(api.setCollector(orderId, request), MarketOrderDto.serializer()).toDomain()
        }

    override suspend fun cancelPurchase(orderId: String): Result<Unit> = call("Failed to cancel order") {
        api.cancelPurchase(orderId)
    }

    override suspend fun getPurchases(): Result<List<MarketOrder>> = call("Failed to load orders") {
        decodeList(api.getPurchases(LIST_PAGE_SIZE), MarketOrderDto.serializer()).map { it.toDomain() }
    }

    override suspend fun getOrder(orderId: String, asSeller: Boolean): Result<MarketOrder> = call("Failed to load order") {
        val body = try {
            if (asSeller) api.getSale(orderId) else api.getPurchase(orderId)
        } catch (e: ResponseException) {
            // The single-order endpoints aren't documented; if they don't exist, find the order
            // in the list instead.
            if (e.response.status.value !in NOT_ROUTED) throw e
            null
        }
        if (body != null) {
            decodeObject(body, MarketOrderDto.serializer()).toDomain()
        } else {
            val list = if (asSeller) api.getSales(LIST_PAGE_SIZE) else api.getPurchases(LIST_PAGE_SIZE)
            decodeList(list, MarketOrderDto.serializer()).map { it.toDomain() }.firstOrNull { it.id == orderId }
                ?: return Result.Error("Order not found")
        }
    }

    // ── Helpers ──

    // Inline so a block can bail out early with its own `return Result.Error(...)`.
    private suspend inline fun <T> call(fallbackMessage: String, block: () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        httpError(e)
    } catch (_: SocketTimeoutException) {
        Result.Error("Connection timed out. Make sure the server is running.")
    } catch (e: Exception) {
        Result.Error(e.message ?: fallbackMessage, e)
    }

    private fun <T> decodeObject(body: JsonElement, serializer: KSerializer<T>): T =
        ApiClient.json.decodeFromJsonElement(serializer, unwrapObject(body))

    private fun <T> decodeList(body: JsonElement, serializer: KSerializer<T>): List<T> =
        unwrapList(body).mapNotNull { element ->
            (element as? JsonObject)?.let { ApiClient.json.decodeFromJsonElement(serializer, it) }
        }

    // {..entity..} or {success, message, data: {..entity..}}
    private fun unwrapObject(body: JsonElement): JsonObject {
        val obj = body as? JsonObject ?: error("Unexpected response")
        val data = obj["data"] as? JsonObject
        return if (data != null && "id" !in obj) data else obj
    }

    // [..], {data: [..]} (paginator / resource collection), or {success, data: {data: [..]}}
    private fun unwrapList(body: JsonElement): JsonArray = when (body) {
        is JsonArray -> body
        is JsonObject -> when (val data = body["data"]) {
            is JsonArray -> data
            is JsonObject -> unwrapList(data)
            else -> JsonArray(emptyList())
        }
        else -> JsonArray(emptyList())
    }

    // The object holding the page's `data` array, plus Laravel resource-collection `meta`.
    private fun paginatorOf(body: JsonElement): JsonObject? {
        val obj = body as? JsonObject ?: return null
        val data = obj["data"]
        if (data is JsonObject) return paginatorOf(data)
        return (obj["meta"] as? JsonObject) ?: obj
    }

    private fun JsonObject.intField(key: String): Int? =
        runCatching { this[key]?.jsonPrimitive?.let { it.intOrNull ?: it.contentOrNull?.toIntOrNull() } }.getOrNull()

    // A 4xx carrying the backend's own message (e.g. "quantity exceeds stock", "collection no
    // longer available") is a business rule the user needs to read verbatim, so it's marked
    // user-facing; 401s and 5xx/unparseable bodies keep going through friendlyError.
    private suspend fun httpError(e: ResponseException): Result.Error {
        val code = e.response.status.value
        val backendMessage = parseBackendMessage(e) ?: return Result.Error("Server error ($code)", e)
        return if (code in 400..499 && code != 401) {
            Result.Error(backendMessage, UserFacingException(backendMessage, e))
        } else {
            Result.Error(backendMessage, e)
        }
    }

    private suspend fun parseBackendMessage(e: ResponseException): String? {
        val body = runCatching { e.response.bodyAsText() }.getOrNull()
        if (body.isNullOrBlank()) return null
        return runCatching {
            val json = Json.parseToJsonElement(body).jsonObject
            val message = json["message"]?.jsonPrimitive?.contentOrNull
            val errors = json["errors"] as? JsonObject
            val fieldMsg = errors?.values?.firstOrNull()?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
            (fieldMsg ?: message)?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private companion object {
        const val LIST_PAGE_SIZE = 100
        val NOT_ROUTED = setOf(404, 405)
    }
}
