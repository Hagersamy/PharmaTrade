package com.pharmatrade.feature.supplierorder.data.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.supplierorder.data.remote.SupplierOrderApi
import com.pharmatrade.feature.supplierorder.data.remote.dto.ConfirmItemRequest
import com.pharmatrade.feature.supplierorder.data.remote.dto.ConfirmOrderRequest
import com.pharmatrade.feature.supplierorder.data.remote.dto.ReportShortageRequest
import com.pharmatrade.feature.supplierorder.data.remote.dto.ShortageItemRequest
import com.pharmatrade.feature.supplierorder.domain.model.ConfirmItemInput
import com.pharmatrade.feature.supplierorder.domain.model.ShortageItemInput
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderDetail
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrdersPage
import com.pharmatrade.feature.supplierorder.domain.repository.SupplierOrderRepository
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SupplierOrderRepositoryImpl(
    private val api: SupplierOrderApi = SupplierOrderApi()
) : SupplierOrderRepository {

    override suspend fun getOrders(status: String?, perPage: Int): Result<SupplierOrdersPage> = try {
        val response = api.getOrders(status = status, perPage = perPage)
        val orders = response.data?.data?.map { it.toDomain() } ?: emptyList()
        val total = response.data?.total ?: orders.size
        Result.Success(SupplierOrdersPage(orders = orders, total = total))
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load orders", e)
    }

    override suspend fun getOrderDetail(orderId: String): Result<SupplierOrderDetail> = try {
        val response = api.getOrderDetail(orderId)
        val order = response.data?.toDomain() ?: return Result.Error("Order not found")
        Result.Success(order)
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
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
    } catch (e: ResponseException) {
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
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to report shortage", e)
    }

    override suspend fun shipOrder(orderId: String): Result<Unit> = try {
        api.shipOrder(orderId)
        Result.Success(Unit)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark order as shipped", e)
    }

    override suspend fun deliverOrder(orderId: String): Result<Unit> = try {
        api.deliverOrder(orderId)
        Result.Success(Unit)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to mark order as delivered", e)
    }

    private suspend fun parseHttpError(e: ResponseException): String {
        val code = e.response.status.value
        val body = runCatching { e.response.bodyAsText() }.getOrNull()
        if (body.isNullOrBlank()) return "Server error ($code)"
        return runCatching {
            val json = Json.parseToJsonElement(body).jsonObject
            val message = json["message"]?.jsonPrimitive?.contentOrNull
            val errors = json["errors"]?.jsonObject
            if (errors != null && errors.isNotEmpty()) {
                val fieldMsg = errors.values.firstOrNull()?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                fieldMsg ?: message ?: "Server error ($code)"
            } else {
                message ?: "Server error ($code)"
            }
        }.getOrDefault("Server error ($code)")
    }
}
