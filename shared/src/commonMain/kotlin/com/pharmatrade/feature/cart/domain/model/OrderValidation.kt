package com.pharmatrade.feature.cart.domain.model

sealed class OrderValidation {
    object Valid : OrderValidation()
    data class BelowMinimum(
        val sellerId: String,
        val sellerName: String,
        val currentAmount: Double,
        val minimumAmount: Double,
        val shortfall: Double
    ) : OrderValidation()
}
