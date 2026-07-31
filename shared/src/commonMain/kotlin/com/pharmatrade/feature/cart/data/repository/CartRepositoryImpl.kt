package com.pharmatrade.feature.cart.data.repository

import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.cart.domain.model.Cart
import com.pharmatrade.feature.cart.domain.model.CartItem
import com.pharmatrade.feature.cart.domain.repository.CartRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val cartMapSerializer = MapSerializer(String.serializer(), ListSerializer(CartItem.serializer()))

// Persists the cart to local storage so items stay in it across app restarts / process death,
// until the user removes them (or clears the cart, or places the order) themselves.
class CartRepositoryImpl(private val settings: Settings? = null) : CartRepository {

    private companion object {
        const val KEY_CART = "cart_items"
    }

    private val items: MutableMap<String, MutableList<CartItem>> = loadPersistedItems()

    private fun loadPersistedItems(): MutableMap<String, MutableList<CartItem>> {
        val stored = settings?.getStringOrNull(KEY_CART) ?: return mutableMapOf()
        val decoded = runCatching { Json.decodeFromString(cartMapSerializer, stored) }.getOrNull()
            ?: return mutableMapOf()
        return decoded.mapValuesTo(mutableMapOf()) { it.value.toMutableList() }
    }

    private fun persist() {
        settings?.putString(KEY_CART, Json.encodeToString(cartMapSerializer, items))
    }

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
        persist()
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
        persist()
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
        persist()
        return getCart()
    }

    private fun removeEmptySellers() {
        val emptySellerIds = items.filterValues { it.isEmpty() }.keys.toList()
        emptySellerIds.forEach { items.remove(it) }
    }

    override fun clearCart(): Cart {
        items.clear()
        persist()
        return getCart()
    }

    override suspend fun placeOrder(cart: Cart): Result<String> {
        delay(1500) // Simulate API call
        val orderId = "ORD-${Clock.System.now().toEpochMilliseconds()}"
        items.clear()
        persist()
        return Result.Success(orderId)
    }
}
