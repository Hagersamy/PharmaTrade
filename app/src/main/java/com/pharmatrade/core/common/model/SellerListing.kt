package com.pharmatrade.core.common.model

data class SellerListing(
    val id: String,
    val drug: Drug,
    val seller: Seller,
    val pricePerUnit: Double,
    val discountPercentage: Double,
    val quantityAvailable: Int,
    val unit: String,
    val expiryDate: String,
    val isActive: Boolean = true
) {
    val finalPrice: Double
        get() = pricePerUnit * (1.0 - discountPercentage / 100.0)

    val savedAmount: Double
        get() = pricePerUnit - finalPrice

    val hasDiscount: Boolean
        get() = discountPercentage > 0.0
}
