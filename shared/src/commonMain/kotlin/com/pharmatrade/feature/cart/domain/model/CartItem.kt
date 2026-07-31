package com.pharmatrade.feature.cart.domain.model

import com.pharmatrade.core.common.model.SellerListing
import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
    val listing: SellerListing,
    val quantity: Int
) {
    val subtotal: Double get() = listing.finalPrice * quantity
}
