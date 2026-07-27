package com.pharmatrade.feature.pharmacyorder.domain.usecase

import com.pharmatrade.core.common.result.Result
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

class GetBranchUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(): Result<Branch> = repository.getBranch()
}

class GetPharmacyOrdersUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(status: String? = null, perPage: Int = 20): Result<OrdersPage> =
        repository.getOrders(status = status, perPage = perPage)
}

class GetSuppliersUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(): Result<List<PharmacySupplier>> = repository.getSuppliers()
}

class GetSupplierInventoryUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(supplierId: String): Result<SupplierInventoryPage> =
        repository.getSupplierInventory(supplierId)
}

class GetAllSuppliersDrugsUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(page: Int = 1): Result<SupplierCatalogPage> = repository.getAllSuppliersDrugs(page)
}

class CreateOrderUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(orderMode: OrderMode, notes: String? = null): Result<OrderDetail> =
        repository.createOrder(orderMode, notes)
}

class AddOrderItemUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(orderId: String, drugId: String, quantity: Int): Result<DraftOrderItem> {
        if (quantity <= 0) return Result.Error("Quantity must be greater than 0")
        return repository.addItem(orderId, drugId, quantity)
    }
}

class RemoveOrderItemUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(orderId: String, itemId: String): Result<Unit> = repository.removeItem(orderId, itemId)
}

class UploadOrderItemsUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(orderId: String, fileUri: String, fileName: String): Result<UploadItemsResult> {
        if (fileUri.isBlank()) return Result.Error("No file selected")
        return repository.uploadItemsFile(orderId, fileUri, fileName)
    }
}

class AllocateOrderUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(orderId: String, supplierId: String? = null): Result<OrderDetail> =
        repository.allocateOrder(orderId, supplierId)
}

class GetOrderDetailUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(orderId: String): Result<OrderDetail> = repository.getOrderDetail(orderId)
}

class ResolveShortageUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(orderId: String, action: String, shortageReportIds: List<String>): Result<Unit> =
        repository.resolveShortage(orderId, action, shortageReportIds)
}

class CancelOrderUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(orderId: String): Result<Unit> = repository.cancelOrder(orderId)
}

class DeliverOrderUseCase(private val repository: PharmacyOrderRepository) {
    suspend operator fun invoke(orderId: String): Result<Unit> = repository.deliverOrder(orderId)
}
