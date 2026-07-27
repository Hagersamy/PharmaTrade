package com.pharmatrade.feature.pharmacyorder.domain.model

data class DraftOrderItem(
    val id: String,
    val drugId: String,
    val drugName: String,
    val quantity: Int
)
