package com.pharmatrade.feature.seller.domain.usecase

import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.seller.domain.repository.SellerRepository

class GetSellerProfileUseCase(private val repository: SellerRepository) {
    suspend operator fun invoke(sellerId: String): Result<Seller> =
        repository.getSellerProfile(sellerId)
}

class GetSellerListingsUseCase(private val repository: SellerRepository) {
    suspend operator fun invoke(sellerId: String): Result<List<SellerListing>> =
        repository.getSellerListings(sellerId)
}

class UpdateListingUseCase(private val repository: SellerRepository) {
    suspend operator fun invoke(
        listingId: String,
        pricePerUnit: Double,
        discountPercentage: Double,
        quantityAvailable: Int,
        expiryDate: String
    ): Result<SellerListing> {
        if (pricePerUnit <= 0) return Result.Error("Price must be greater than 0")
        if (discountPercentage < 0 || discountPercentage >= 100) return Result.Error("Discount must be 0-99%")
        if (quantityAvailable <= 0) return Result.Error("Quantity must be greater than 0")
        return repository.updateListing(listingId, pricePerUnit, discountPercentage, quantityAvailable, expiryDate)
    }
}

class DeleteListingUseCase(private val repository: SellerRepository) {
    suspend operator fun invoke(listingId: String): Result<Unit> =
        repository.deleteListing(listingId)
}

class UpdateMinimumOrderUseCase(private val repository: SellerRepository) {
    suspend operator fun invoke(sellerId: String, amount: Double): Result<Seller> {
        if (amount < 0) return Result.Error("Minimum order amount cannot be negative")
        return repository.updateMinimumOrderAmount(sellerId, amount)
    }
}
