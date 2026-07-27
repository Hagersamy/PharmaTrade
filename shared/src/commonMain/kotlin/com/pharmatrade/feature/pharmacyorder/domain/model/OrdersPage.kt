package com.pharmatrade.feature.pharmacyorder.domain.model

data class OrdersPage(
    val orders: List<PharmacyOrderSummary>,
    val total: Int
)
