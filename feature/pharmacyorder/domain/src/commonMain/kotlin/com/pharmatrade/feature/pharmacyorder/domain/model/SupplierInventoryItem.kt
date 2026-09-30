package com.pharmatrade.feature.pharmacyorder.domain.model

data class SupplierInventoryItem(
    val id: String,
    val drugId: String,
    val drugName: String,
    val dosageForm: String = "",
    val strength: String = "",
    val quantityAvailable: Int,
    val unitPrice: Double,
    val discountPct: Double,
    val effectivePrice: Double
)
