package com.pharmatrade.feature.seller.data.repository

import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.seller.data.fake.FakeSellerData
import com.pharmatrade.feature.seller.domain.repository.SellerRepository
import kotlinx.coroutines.delay

class SellerRepositoryImpl : SellerRepository {

    override suspend fun getSellerProfile(sellerId: String): Result<Seller> {
        delay(500)
        val seller = FakeSellerData.getSellerById(sellerId)
            ?: return Result.Error("Seller not found")
        return Result.Success(seller)
    }

    override suspend fun getSellerListings(sellerId: String): Result<List<SellerListing>> {
        delay(700)
        return Result.Success(FakeSellerData.getListingsBySellerIdl(sellerId))
    }

    override suspend fun updateListing(
        listingId: String, pricePerUnit: Double, discountPercentage: Double,
        quantityAvailable: Int, expiryDate: String
    ): Result<SellerListing> {
        delay(800)
        val index = FakeSellerData.listings.indexOfFirst { it.id == listingId }
        if (index == -1) return Result.Error("Listing not found")
        val existing = FakeSellerData.listings[index]
        val updated = existing.copy(
            pricePerUnit = pricePerUnit,
            discountPercentage = discountPercentage,
            quantityAvailable = quantityAvailable,
            expiryDate = expiryDate
        )
        FakeSellerData.listings[index] = updated
        return Result.Success(updated)
    }

    override suspend fun deleteListing(listingId: String): Result<Unit> {
        delay(600)
        val index = FakeSellerData.listings.indexOfFirst { it.id == listingId }
        if (index == -1) return Result.Error("Listing not found")
        FakeSellerData.listings.removeAt(index)
        return Result.Success(Unit)
    }

    override suspend fun updateMinimumOrderAmount(sellerId: String, amount: Double): Result<Seller> {
        delay(500)
        val index = FakeSellerData.sellers.indexOfFirst { it.id == sellerId }
        if (index == -1) return Result.Error("Seller not found")
        val updated = FakeSellerData.sellers[index].copy(minimumOrderAmount = amount)
        FakeSellerData.sellers[index] = updated
        return Result.Success(updated)
    }
}
