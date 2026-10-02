package com.pharmatrade.feature.pharmacyorder.data.repository

import com.pharmatrade.core.common.error.UserFacingException
import com.pharmatrade.core.common.log.debugLog
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.io.PlatformFileReader
import com.pharmatrade.core.network.FormFile
import com.pharmatrade.feature.pharmacyorder.data.remote.PharmacyOrderApi
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.AddItemRequest
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.AllocateRequest
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.CreateOrderRequest
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.ConfirmDeliveryRequest
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.ResolveShortageRequest
import com.pharmatrade.feature.pharmacyorder.domain.model.Branch
import com.pharmatrade.feature.pharmacyorder.domain.model.DraftOrderItem
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderDetail
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderMode
import com.pharmatrade.feature.pharmacyorder.domain.model.OrdersPage
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacySupplier
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogPage
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierInventoryPage
import com.pharmatrade.feature.pharmacyorder.domain.model.UploadItemsResult
import com.pharmatrade.feature.pharmacyorder.domain.repository.PharmacyOrderRepository
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// logcat: adb logcat -s PharmaSearch
private const val SEARCH_LOG_TAG = "PharmaSearch"

class PharmacyOrderRepositoryImpl(
    private val fileReader: PlatformFileReader,
    private val api: PharmacyOrderApi = PharmacyOrderApi()
) : PharmacyOrderRepository {

    override suspend fun getBranch(): Result<Branch> = try {
        val response = api.getBranch()
        val branch = response.data?.toDomain() ?: return Result.Error("Branch not found")
        Result.Success(branch)
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load branch", e)
    }

    override suspend fun getOrders(status: String?, perPage: Int): Result<OrdersPage> = try {
        val response = api.getOrders(status = status, perPage = perPage)
        val orders = response.data?.data?.map { it.toDomain() } ?: emptyList()
        val total = response.data?.total ?: orders.size
        Result.Success(OrdersPage(orders = orders, total = total))
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load orders", e)
    }

    override suspend fun getSuppliers(): Result<List<PharmacySupplier>> = try {
        val response = api.getSuppliers()
        val suppliers = response.data?.map { it.toDomain() } ?: emptyList()
        Result.Success(suppliers)
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load suppliers", e)
    }

    override suspend fun getSupplierInventory(supplierId: String): Result<SupplierInventoryPage> = try {
        val response = api.getSupplierInventory(supplierId)
        val supplier = response.data?.supplier?.toDomain()
        // "id" is only ever used locally as a row/UI key (never sent back to the server), but
        // the backend has been seen to omit or duplicate it across rows on this endpoint, which
        // collapses every row onto the same key and makes per-row UI state (quick-add spinners,
        // added-quantity badges) apply to all rows at once. Suffix with the row's position so
        // the key is always unique, regardless of what the backend actually sends.
        val items = response.data?.inventory?.data
            ?.mapIndexed { index, dto -> dto.toDomain().let { it.copy(id = "${it.id}_$index") } }
            ?: emptyList()
        Result.Success(SupplierInventoryPage(supplier = supplier, items = items))
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load supplier inventory", e)
    }

    override suspend fun getAllSuppliersDrugs(page: Int, search: String?, perPage: Int?): Result<SupplierCatalogPage> = try {
        debugLog(SEARCH_LOG_TAG, "API → GET pharmacy/suppliers/drugs search=\"${search.orEmpty()}\" page=$page per_page=$perPage")
        val response = api.getAllSuppliersDrugs(page = page, search = search, perPage = perPage)
        val dto = response.data
        // Unlike getSupplierInventory (a single one-shot fetch), this is called once per page as
        // the buyer scrolls, and `mapIndexed`'s index restarts at 0 on every call — suffixing with
        // it here would let page 2's items collide onto the same ids as page 1's (same backend id
        // + same local index), making the cart's added-quantity badges follow list position across
        // pages instead of the actual item. supplierId + drugId is stable across pages instead.
        val items = dto?.data
            ?.map { itemDto -> itemDto.toDomain().let { it.copy(id = "${it.supplierId}_${it.drugId}") } }
            ?: emptyList()
        val currentPage = dto?.currentPage ?: page
        val lastPage = dto?.lastPage ?: page
        debugLog(
            SEARCH_LOG_TAG,
            "API ← search=\"${search.orEmpty()}\" page=$page: ${items.size} items, total=${dto?.total}, " +
                "current_page=${dto?.currentPage}, last_page=${dto?.lastPage}, has_next_page=${dto?.hasNextPage}"
        )
        items.forEach { debugLog(SEARCH_LOG_TAG, "   • id=${it.id} \"${it.drugName}\" supplier=${it.supplierName} effective=${it.effectivePrice}") }
        Result.Success(
            SupplierCatalogPage(
                items = items,
                currentPage = currentPage,
                lastPage = lastPage,
                total = dto?.total ?: items.size,
                hasNextPage = dto?.hasNextPage ?: (currentPage < lastPage)
            )
        )
    } catch (e: CancellationException) {
        // A newer search replaced this one — let the cancellation propagate instead of turning
        // it into an error result.
        debugLog(SEARCH_LOG_TAG, "API ✕ search=\"${search.orEmpty()}\" page=$page cancelled (newer query)")
        throw e
    } catch (e: ResponseException) {
        debugLog(SEARCH_LOG_TAG, "API ✕ search=\"${search.orEmpty()}\" page=$page HTTP ${e.response.status.value}")
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        debugLog(SEARCH_LOG_TAG, "API ✕ search=\"${search.orEmpty()}\" page=$page ${e::class.simpleName}: ${e.message}")
        Result.Error(e.message ?: "Failed to load supplier drugs", e)
    }

    override suspend fun createOrder(orderMode: OrderMode, notes: String?): Result<OrderDetail> = try {
        val response = api.createOrder(CreateOrderRequest(orderMode = orderMode.apiValue, notes = notes?.takeIf { it.isNotBlank() }))
        val order = response.data?.toDomain() ?: return Result.Error("Server did not return the created order")
        Result.Success(order)
    } catch (e: ResponseException) {
        httpError(e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to create order", e)
    }

    override suspend fun addItem(orderId: String, drugId: String, quantity: Int): Result<DraftOrderItem> = try {
        val response = api.addItem(orderId, AddItemRequest(drugId = drugId, quantity = quantity))
        val item = response.data?.toDomain() ?: return Result.Error("Server did not return the added item")
        Result.Success(item)
    } catch (e: ResponseException) {
        httpError(e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to add item", e)
    }

    override suspend fun removeItem(orderId: String, itemId: String): Result<Unit> = try {
        api.removeItem(orderId, itemId)
        Result.Success(Unit)
    } catch (e: ResponseException) {
        httpError(e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to remove item", e)
    }

    override suspend fun uploadItemsFile(orderId: String, fileUri: String, fileName: String): Result<UploadItemsResult> = try {
        val info = fileReader.read(fileUri) ?: return Result.Error("Could not read the selected file.")
        val resolvedName = info.displayName.ifBlank { fileName }

        val ext = resolvedName.substringAfterLast('.', "").lowercase()
        val mime = when (ext) {
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "csv" -> "text/csv"
            else -> info.mimeType ?: "application/octet-stream"
        }

        val response = api.uploadItems(orderId, FormFile(bytes = info.bytes, fileName = resolvedName, mimeType = mime))
        val result = response.data?.toDomain() ?: UploadItemsResult(addedCount = 0)
        Result.Success(result)
    } catch (e: ResponseException) {
        httpError(e)
    } catch (_: SocketTimeoutException) {
        Result.Error("Connection timed out. Make sure the server is running.")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Upload failed", e)
    }

    override suspend fun allocateOrder(orderId: String, supplierId: String?): Result<OrderDetail> = try {
        val response = api.allocateOrder(orderId, AllocateRequest(supplierId = supplierId))
        val order = response.data?.toDomain() ?: return Result.Error("Server did not return the allocation result")
        Result.Success(order)
    } catch (e: ResponseException) {
        httpError(e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to allocate order", e)
    }

    override suspend fun getOrderDetail(orderId: String): Result<OrderDetail> = try {
        val response = api.getOrderDetail(orderId)
        val order = response.data?.toDomain() ?: return Result.Error("Order not found")
        Result.Success(order)
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load order", e)
    }

    override suspend fun resolveShortage(
        orderId: String,
        action: String,
        shortageReportIds: List<String>
    ): Result<Unit> = try {
        api.resolveShortage(
            orderId,
            ResolveShortageRequest(
                action = action,
                shortageReportIds = shortageReportIds.mapNotNull { it.toIntOrNull() }
            )
        )
        Result.Success(Unit)
    } catch (e: ResponseException) {
        httpError(e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to resolve shortage", e)
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> = try {
        api.cancelOrder(orderId)
        Result.Success(Unit)
    } catch (e: ResponseException) {
        httpError(e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to cancel order", e)
    }

    override suspend fun deliverOrder(orderId: String, shortageReportIds: List<String>): Result<Unit> = try {
        api.confirmDelivery(
            orderId,
            ConfirmDeliveryRequest(
                action = "confirm",
                shortageReportIds = shortageReportIds.mapNotNull { it.toIntOrNull() }
            )
        )
        Result.Success(Unit)
    } catch (e: ResponseException) {
        httpError(e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark order as delivered", e)
    }

    // A 4xx carrying the backend's own message (e.g. allocate rejected because the supplier's
    // minimum order isn't met) is a business rule the buyer needs to read verbatim, so it's
    // marked user-facing; 401s and 5xx/unparseable bodies keep going through friendlyError.
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
}
