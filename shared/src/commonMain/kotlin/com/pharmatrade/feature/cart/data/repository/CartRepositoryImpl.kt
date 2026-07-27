package com.pharmatrade.feature.cart.data.repository

import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.cart.domain.model.Cart
import com.pharmatrade.feature.cart.domain.model.CartItem
import com.pharmatrade.feature.cart.domain.repository.CartRepository
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

class CartRepositoryImpl : CartRepository {

    private val items = mutableMapOf<String, MutableList<CartItem>>()

    override fun getCart(): Cart = Cart(items.mapValues { it.value.toList() })

    override fun addItem(listing: SellerListing, quantity: Int): Cart {
        val sellerId = listing.seller.id
        val sellerItems = items.getOrPut(sellerId) { mutableListOf() }
        val existingIndex = sellerItems.indexOfFirst { it.listing.id == listing.id }
        if (existingIndex >= 0) {
            val existing = sellerItems[existingIndex]
            sellerItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            sellerItems.add(CartItem(listing = listing, quantity = quantity))
        }
        return getCart()
    }

    override fun updateItemQuantity(listingId: String, quantity: Int): Cart {
        for ((_, sellerItems) in items) {
            val index = sellerItems.indexOfFirst { it.listing.id == listingId }
            if (index >= 0) {
                if (quantity <= 0) {
                    sellerItems.removeAt(index)
                } else {
                    sellerItems[index] = sellerItems[index].copy(quantity = quantity)
                }
                break
            }
        }
        removeEmptySellers()
        return getCart()
    }

    override fun removeItem(listingId: String): Cart {
        for ((_, sellerItems) in items) {
            val index = sellerItems.indexOfFirst { it.listing.id == listingId }
            if (index >= 0) {
                sellerItems.removeAt(index)
                break
            }
        }
        removeEmptySellers()
        return getCart()
    }

    private fun removeEmptySellers() {
        val emptySellerIds = items.filterValues { it.isEmpty() }.keys.toList()
        emptySellerIds.forEach { items.remove(it) }
    }

    override fun clearCart(): Cart {
        items.clear()
        return getCart()
    }

    override suspend fun placeOrder(cart: Cart): Result<String> {
        delay(1500) // Simulate API call
        val orderId = "ORD-${Clock.System.now().toEpochMilliseconds()}"
        items.clear()
        return Result.Success(orderId)
    }
}
