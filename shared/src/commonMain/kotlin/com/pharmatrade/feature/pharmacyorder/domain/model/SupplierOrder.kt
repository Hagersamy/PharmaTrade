package com.pharmatrade.feature.pharmacyorder.domain.model

data class SupplierOrder(
    val supplierId: String,
    val supplierName: String,
    val subtotal: Double,
    val status: String,
    val lines: List<OrderDrugLine> = emptyList()
)
