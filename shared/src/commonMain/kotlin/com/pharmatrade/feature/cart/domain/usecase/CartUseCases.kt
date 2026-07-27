package com.pharmatrade.feature.cart.domain.usecase

import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.cart.domain.model.Cart
import com.pharmatrade.feature.cart.domain.model.OrderValidation
import com.pharmatrade.feature.cart.domain.repository.CartRepository

class GetCartUseCase(private val repository: CartRepository) {
    operator fun invoke(): Cart = repository.getCart()
}

class AddToCartUseCase(private val repository: CartRepository) {
    operator fun invoke(listing: SellerListing, quantity: Int = 1): Cart =
        repository.addItem(listing, quantity)
}

class UpdateCartItemUseCase(private val repository: CartRepository) {
    operator fun invoke(listingId: String, quantity: Int): Cart =
        repository.updateItemQuantity(listingId, quantity)
}

class RemoveFromCartUseCase(private val repository: CartRepository) {
    operator fun invoke(listingId: String): Cart =
        repository.removeItem(listingId)
}

class ClearCartUseCase(private val repository: CartRepository) {
    operator fun invoke(): Cart = repository.clearCart()
}

class ValidateCartUseCase {
    operator fun invoke(cart: Cart): List<OrderValidation> {
        if (cart.isEmpty()) return emptyList()
        val violations = mutableListOf<OrderValidation>()
        for ((sellerId, items) in cart.itemsBySeller) {
            if (items.isEmpty()) continue
            val seller = items.first().listing.seller
            val sellerTotal = items.sumOf { it.subtotal }
            if (sellerTotal < seller.minimumOrderAmount) {
                violations.add(
                    OrderValidation.BelowMinimum(
                        sellerId = sellerId,
                        sellerName = seller.businessName,
                        currentAmount = sellerTotal,
                        minimumAmount = seller.minimumOrderAmount,
                        shortfall = seller.minimumOrderAmount - sellerTotal
                    )
                )
            }
        }
        return violations
    }
}

class PlaceOrderUseCase(private val repository: CartRepository) {
    suspend operator fun invoke(cart: Cart): Result<String> =
        repository.placeOrder(cart)
}
