package com.pharmatrade.feature.pharmacyorder.domain.model

data class PharmacyOrderSummary(
    val id: String,
    val orderNumber: String,
    val date: String,
    val totalValue: Double,
    val status: String,
    val supplierCount: Int
)
