package com.pharmatrade.feature.supplierorder.data.repository

import com.google.gson.JsonParser
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.network.RetrofitClient
import com.pharmatrade.feature.supplierorder.data.remote.SupplierOrderApiService
import com.pharmatrade.feature.supplierorder.data.remote.dto.ConfirmItemRequest
import com.pharmatrade.feature.supplierorder.data.remote.dto.ConfirmOrderRequest
import com.pharmatrade.feature.supplierorder.data.remote.dto.ReportShortageRequest
import com.pharmatrade.feature.supplierorder.data.remote.dto.ShortageItemRequest
import com.pharmatrade.feature.supplierorder.domain.model.ConfirmItemInput
import com.pharmatrade.feature.supplierorder.domain.model.ShortageItemInput
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderDetail
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrdersPage
import com.pharmatrade.feature.supplierorder.domain.repository.SupplierOrderRepository
import retrofit2.HttpException

class SupplierOrderRepositoryImpl : SupplierOrderRepository {

    private val api = RetrofitClient.create<SupplierOrderApiService>()

    override suspend fun getOrders(status: String?, perPage: Int): Result<SupplierOrdersPage> = try {
        val response = api.getOrders(status = status, perPage = perPage)
        val orders = response.data?.data?.map { it.toDomain() } ?: emptyList()
        val total = response.data?.total ?: orders.size
        Result.Success(SupplierOrdersPage(orders = orders, total = total))
    } catch (e: HttpException) {
        Result.Error("Server error (${e.code()})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load orders", e)
    }

    override suspend fun getOrderDetail(orderId: String): Result<SupplierOrderDetail> = try {
        val response = api.getOrderDetail(orderId)
        val order = response.data?.toDomain() ?: return Result.Error("Order not found")
        Result.Success(order)
    } catch (e: HttpException) {
        Result.Error("Server error (${e.code()})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load order", e)
    }

    override suspend fun confirmOrder(orderId: String, items: List<ConfirmItemInput>): Result<Unit> = try {
        api.confirmOrder(
            orderId,
            ConfirmOrderRequest(
                items.map {
                    ConfirmItemRequest(
                        orderItemId = it.orderItemId.toIntOrNull() ?: 0,
                        quantityConfirmed = it.quantityConfirmed
                    )
                }
            )
        )
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to confirm order", e)
    }

    override suspend fun reportShortage(orderId: String, items: List<ShortageItemInput>): Result<Unit> = try {
        api.reportShortage(
            orderId,
            ReportShortageRequest(
                items.map {
                    ShortageItemRequest(
                        orderItemId = it.orderItemId.toIntOrNull() ?: 0,
                        quantityConfirmed = it.quantityConfirmed,
                        quantityShort = it.quantityShort,
                        notes = it.notes?.takeIf { n -> n.isNotBlank() }
                    )
                }
            )
        )
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to report shortage", e)
    }

    override suspend fun shipOrder(orderId: String): Result<Unit> = try {
        api.shipOrder(orderId)
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark order as shipped", e)
    }

    override suspend fun deliverOrder(orderId: String): Result<Unit> = try {
        api.deliverOrder(orderId)
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark order as delivered", e)
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
