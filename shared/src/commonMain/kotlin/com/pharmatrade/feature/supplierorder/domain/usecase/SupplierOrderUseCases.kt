package com.pharmatrade.feature.supplierorder.domain.usecase

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.supplierorder.domain.model.ConfirmItemInput
import com.pharmatrade.feature.supplierorder.domain.model.ShortageItemInput
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderDetail
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrdersPage
import com.pharmatrade.feature.supplierorder.domain.repository.SupplierOrderRepository

class GetSupplierOrdersUseCase(private val repository: SupplierOrderRepository) {
    suspend operator fun invoke(status: String? = null, perPage: Int = 20): Result<SupplierOrdersPage> =
        repository.getOrders(status, perPage)
}

class GetSupplierOrderDetailUseCase(private val repository: SupplierOrderRepository) {
    suspend operator fun invoke(orderId: String): Result<SupplierOrderDetail> = repository.getOrderDetail(orderId)
}

class ConfirmSupplierOrderUseCase(private val repository: SupplierOrderRepository) {
    suspend operator fun invoke(orderId: String, items: List<ConfirmItemInput>): Result<Unit> =
        repository.confirmOrder(orderId, items)
}

class ReportSupplierOrderShortageUseCase(private val repository: SupplierOrderRepository) {
    suspend operator fun invoke(orderId: String, items: List<ShortageItemInput>): Result<Unit> =
        repository.reportShortage(orderId, items)
}

class ShipSupplierOrderUseCase(private val repository: SupplierOrderRepository) {
    suspend operator fun invoke(orderId: String): Result<Unit> = repository.shipOrder(orderId)
}

class DeliverSupplierOrderUseCase(private val repository: SupplierOrderRepository) {
    suspend operator fun invoke(orderId: String): Result<Unit> = repository.deliverOrder(orderId)
}
