package com.pharmatrade.feature.pharmacyorder.domain.model

data class OrderDrugLine(
    val drugName: String,
    val qtyRequested: Int,
    val qtyConfirmed: Int?,
    val unitPrice: Double,
    val lineTotal: Double,
    val discountPct: Double = 0.0,
    val status: String = ""
) {
    val isShort: Boolean
        get() = status.equals("short", ignoreCase = true)
}
