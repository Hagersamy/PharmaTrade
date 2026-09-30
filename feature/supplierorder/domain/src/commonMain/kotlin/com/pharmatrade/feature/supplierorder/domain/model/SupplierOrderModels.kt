package com.pharmatrade.feature.supplierorder.domain.model

data class SupplierOrderSummary(
    val id: String,
    val orderNumber: String,
    val status: String,
    val subtotal: Double,
    val commissionPct: Double,
    val commissionValue: Double,
    val createdAt: String,
    val pharmacyBranchName: String,
    val pharmacyBranchAddress: String,
    val pharmacyBranchPhone: String
)

data class SupplierOrdersPage(
    val orders: List<SupplierOrderSummary>,
    val total: Int
)

data class SupplierOrderDetail(
    val id: String,
    val orderNumber: String,
    val status: String,
    val subtotal: Double,
    val commissionPct: Double,
    val commissionValue: Double,
    val confirmedAt: String?,
    val shippedAt: String?,
    val deliveredAt: String?,
    val pharmacyBranchName: String,
    val pharmacyBranchAddress: String,
    val pharmacyBranchPhone: String,
    val items: List<SupplierOrderItem>
) {
    val canConfirm: Boolean get() = status == "pending"
    val canReportShortage: Boolean get() = status == "pending"
    val canShip: Boolean get() = status == "confirmed"
    val canDeliver: Boolean get() = status == "shipped"
}

data class SupplierOrderItem(
    val id: String,
    val drugName: String,
    val dosageForm: String?,
    val strength: String?,
    val quantityRequested: Int,
    val quantityConfirmed: Int?,
    val unitPrice: Double,
    val discountPct: Double,
    val lineTotal: Double,
    val status: String
)

data class ConfirmItemInput(val orderItemId: String, val quantityConfirmed: Int)

data class ShortageItemInput(
    val orderItemId: String,
    val quantityConfirmed: Int,
    val quantityShort: Int,
    val notes: String?
)
