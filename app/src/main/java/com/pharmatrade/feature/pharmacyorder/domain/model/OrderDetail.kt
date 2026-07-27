package com.pharmatrade.feature.pharmacyorder.domain.model

data class OrderDetail(
    val id: String,
    val orderNumber: String,
    val date: String,
    val status: String,
    val totalValue: Double,
    val savings: Double = 0.0,
    val platformFeeNote: String = "Includes 1% platform fee",
    val items: List<DraftOrderItem> = emptyList(),
    val supplierOrders: List<SupplierOrder> = emptyList(),
    val hasUnresolvedShortage: Boolean = false,
    val shortageItemCount: Int = 0,
    val shortageReportIds: List<String> = emptyList()
) {
    val canCancel: Boolean
        get() = status in setOf("draft", "pending_supplier_confirmation")
}
