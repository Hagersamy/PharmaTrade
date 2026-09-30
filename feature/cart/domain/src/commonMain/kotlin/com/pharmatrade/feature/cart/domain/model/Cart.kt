package com.pharmatrade.feature.cart.domain.model

data class Cart(
    val itemsBySeller: Map<String, List<CartItem>> = emptyMap()
) {
    fun getSellerItems(sellerId: String): List<CartItem> =
        itemsBySeller[sellerId] ?: emptyList()

    fun getSellerSubtotal(sellerId: String): Double =
        getSellerItems(sellerId).sumOf { it.subtotal }

    fun getTotalAmount(): Double =
        itemsBySeller.values.flatten().sumOf { it.subtotal }

    fun getTotalItems(): Int =
        itemsBySeller.values.flatten().sumOf { it.quantity }

    fun isEmpty(): Boolean =
        itemsBySeller.isEmpty() || itemsBySeller.values.all { it.isEmpty() }

    fun getAllItems(): List<CartItem> =
        itemsBySeller.values.flatten()
}
