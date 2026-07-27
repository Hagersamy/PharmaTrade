package com.pharmatrade.feature.pharmacyorder.domain.repository

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

interface PharmacyOrderRepository {
    suspend fun getBranch(): Result<Branch>
    suspend fun getOrders(status: String?, perPage: Int): Result<OrdersPage>
    suspend fun getSuppliers(): Result<List<PharmacySupplier>>
    suspend fun getSupplierInventory(supplierId: String): Result<SupplierInventoryPage>
    suspend fun getAllSuppliersDrugs(page: Int): Result<SupplierCatalogPage>
    suspend fun createOrder(orderMode: OrderMode, notes: String? = null): Result<OrderDetail>
    suspend fun addItem(orderId: String, drugId: String, quantity: Int): Result<DraftOrderItem>
    suspend fun removeItem(orderId: String, itemId: String): Result<Unit>
    suspend fun uploadItemsFile(orderId: String, fileUri: String, fileName: String): Result<UploadItemsResult>
    suspend fun allocateOrder(orderId: String, supplierId: String?): Result<OrderDetail>
    suspend fun getOrderDetail(orderId: String): Result<OrderDetail>
    suspend fun resolveShortage(orderId: String, action: String, shortageReportIds: List<String>): Result<Unit>
    suspend fun cancelOrder(orderId: String): Result<Unit>
    suspend fun deliverOrder(orderId: String): Result<Unit>
}
