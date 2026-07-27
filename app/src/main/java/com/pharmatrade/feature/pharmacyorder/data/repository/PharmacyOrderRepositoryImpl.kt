package com.pharmatrade.feature.pharmacyorder.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.JsonParser
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.network.RetrofitClient
import com.pharmatrade.feature.pharmacyorder.data.remote.PharmacyOrderApiService
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.AddItemRequest
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.AllocateRequest
import com.pharmatrade.feature.pharmacyorder.data.remote.dto.CreateOrderRequest
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.SocketTimeoutException

class PharmacyOrderRepositoryImpl(private val context: Context) : PharmacyOrderRepository {

    private val api = RetrofitClient.create<PharmacyOrderApiService>()

    override suspend fun getBranch(): Result<Branch> = try {
        val response = api.getBranch()
        val branch = response.data?.toDomain() ?: return Result.Error("Branch not found")
        Result.Success(branch)
    } catch (e: HttpException) {
        Result.Error("Server error (${e.code()})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load branch", e)
    }

    override suspend fun getOrders(status: String?, perPage: Int): Result<OrdersPage> = try {
        val response = api.getOrders(status = status, perPage = perPage)
        val orders = response.data?.data?.map { it.toDomain() } ?: emptyList()
        val total = response.data?.total ?: orders.size
        Result.Success(OrdersPage(orders = orders, total = total))
    } catch (e: HttpException) {
        Result.Error("Server error (${e.code()})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load orders", e)
    }

    override suspend fun getSuppliers(): Result<List<PharmacySupplier>> = try {
        val response = api.getSuppliers()
        val suppliers = response.data?.map { it.toDomain() } ?: emptyList()
        Result.Success(suppliers)
    } catch (e: HttpException) {
        Result.Error("Server error (${e.code()})", e)
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
    } catch (e: HttpException) {
        Result.Error("Server error (${e.code()})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load supplier inventory", e)
    }

    override suspend fun getAllSuppliersDrugs(page: Int): Result<SupplierCatalogPage> = try {
        val response = api.getAllSuppliersDrugs(page = page)
        val dto = response.data
        // Same defensive id-uniqueness fix as getSupplierInventory above.
        val items = dto?.data
            ?.mapIndexed { index, itemDto -> itemDto.toDomain().let { it.copy(id = "${it.id}_$index") } }
            ?: emptyList()
        Result.Success(
            SupplierCatalogPage(
                items = items,
                currentPage = dto?.currentPage ?: page,
                lastPage = dto?.lastPage ?: page,
                total = dto?.total ?: items.size
            )
        )
    } catch (e: HttpException) {
        Result.Error("Server error (${e.code()})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load supplier drugs", e)
    }

    override suspend fun createOrder(orderMode: OrderMode, notes: String?): Result<OrderDetail> = try {
        val response = api.createOrder(CreateOrderRequest(orderMode = orderMode.apiValue, notes = notes?.takeIf { it.isNotBlank() }))
        val order = response.data?.toDomain() ?: return Result.Error("Server did not return the created order")
        Result.Success(order)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to create order", e)
    }

    override suspend fun addItem(orderId: String, drugId: String, quantity: Int): Result<DraftOrderItem> = try {
        val response = api.addItem(orderId, AddItemRequest(drugId = drugId, quantity = quantity))
        val item = response.data?.toDomain() ?: return Result.Error("Server did not return the added item")
        Result.Success(item)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to add item", e)
    }

    override suspend fun removeItem(orderId: String, itemId: String): Result<Unit> = try {
        api.removeItem(orderId, itemId)
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to remove item", e)
    }

    override suspend fun uploadItemsFile(orderId: String, fileUri: String, fileName: String): Result<UploadItemsResult> = try {
        val uri = Uri.parse(fileUri)
        val resolvedName = resolveFileName(uri).ifBlank { fileName }

        val stream = context.contentResolver.openInputStream(uri)
            ?: return Result.Error("Could not read the selected file.")
        val bytes = stream.use { it.readBytes() }

        val ext = resolvedName.substringAfterLast('.', "").lowercase()
        val mime = when (ext) {
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "csv" -> "text/csv"
            else -> context.contentResolver.getType(uri) ?: "application/octet-stream"
        }

        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", resolvedName, body)
        val response = api.uploadItems(orderId, part)
        val result = response.data?.toDomain() ?: UploadItemsResult(addedCount = 0)
        Result.Success(result)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (_: SocketTimeoutException) {
        Result.Error("Connection timed out. Make sure the server is running.")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Upload failed", e)
    }

    override suspend fun allocateOrder(orderId: String, supplierId: String?): Result<OrderDetail> = try {
        val response = api.allocateOrder(orderId, AllocateRequest(supplierId = supplierId))
        val order = response.data?.toDomain() ?: return Result.Error("Server did not return the allocation result")
        Result.Success(order)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to allocate order", e)
    }

    override suspend fun getOrderDetail(orderId: String): Result<OrderDetail> = try {
        val response = api.getOrderDetail(orderId)
        val order = response.data?.toDomain() ?: return Result.Error("Order not found")
        Result.Success(order)
    } catch (e: HttpException) {
        Result.Error("Server error (${e.code()})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load order", e)
    }

    override suspend fun resolveShortage(orderId: String, action: String, shortageReportIds: List<String>): Result<Unit> = try {
        api.resolveShortage(
            orderId,
            ResolveShortageRequest(action = action, shortageReportIds = shortageReportIds.mapNotNull { it.toIntOrNull() })
        )
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to resolve shortage", e)
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> = try {
        api.cancelOrder(orderId)
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to cancel order", e)
    }

    override suspend fun deliverOrder(orderId: String): Result<Unit> = try {
        api.confirmDelivery(orderId, ResolveShortageRequest(action = "confirm", shortageReportIds = emptyList()))
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark order as delivered", e)
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col != -1 && cursor.moveToFirst()) return cursor.getString(col)
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: ""
    }

    private fun parseHttpError(e: HttpException): String {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        if (body.isNullOrBlank()) return "Server error (${e.code()})"
        return runCatching {
            val json = JsonParser.parseString(body).asJsonObject
            val message = json.get("message")?.asString
            val errors = json.getAsJsonObject("errors")
            if (errors != null && errors.size() > 0) {
                val first = errors.entrySet().first()
                val fieldMsg = first.value.asJsonArray.firstOrNull()?.asString
                fieldMsg ?: message ?: "Server error (${e.code()})"
            } else {
                message ?: "Server error (${e.code()})"
            }
        }.getOrDefault("Server error (${e.code()})")
    }
}
