package com.pharmatrade.feature.cart.domain.repository

import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.cart.domain.model.Cart

interface CartRepository {
    fun getCart(): Cart
    fun addItem(listing: SellerListing, quantity: Int): Cart
    fun updateItemQuantity(listingId: String, quantity: Int): Cart
    fun removeItem(listingId: String): Cart
    fun clearCart(): Cart
    suspend fun placeOrder(cart: Cart): Result<String>
}
