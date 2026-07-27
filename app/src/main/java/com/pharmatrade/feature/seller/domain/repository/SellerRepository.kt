package com.pharmatrade.feature.seller.domain.repository

import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.result.Result

interface SellerRepository {
    suspend fun getSellerProfile(sellerId: String): Result<Seller>
    suspend fun getSellerListings(sellerId: String): Result<List<SellerListing>>
    suspend fun updateListing(
        listingId: String,
        pricePerUnit: Double,
        discountPercentage: Double,
        quantityAvailable: Int,
        expiryDate: String
    ): Result<SellerListing>
    suspend fun deleteListing(listingId: String): Result<Unit>
    suspend fun updateMinimumOrderAmount(sellerId: String, amount: Double): Result<Seller>
}
