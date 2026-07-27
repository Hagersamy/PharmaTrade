package com.pharmatrade.feature.supplierorder.domain.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.supplierorder.domain.model.ConfirmItemInput
import com.pharmatrade.feature.supplierorder.domain.model.ShortageItemInput
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderDetail
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrdersPage

interface SupplierOrderRepository {
    suspend fun getOrders(status: String? = null, perPage: Int = 20): Result<SupplierOrdersPage>
    suspend fun getOrderDetail(orderId: String): Result<SupplierOrderDetail>
    suspend fun confirmOrder(orderId: String, items: List<ConfirmItemInput>): Result<Unit>
    suspend fun reportShortage(orderId: String, items: List<ShortageItemInput>): Result<Unit>
    suspend fun shipOrder(orderId: String): Result<Unit>
    suspend fun deliverOrder(orderId: String): Result<Unit>
}
