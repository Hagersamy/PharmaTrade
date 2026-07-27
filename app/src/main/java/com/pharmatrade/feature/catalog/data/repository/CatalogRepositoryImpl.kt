package com.pharmatrade.feature.catalog.data.repository

import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.catalog.domain.repository.CatalogRepository
import com.pharmatrade.feature.seller.data.fake.FakeSellerData
import kotlinx.coroutines.delay

class CatalogRepositoryImpl : CatalogRepository {

    override suspend fun getAllSellers(): Result<List<Seller>> {
        delay(600)
        return Result.Success(FakeSellerData.sellers.toList())
    }

    override suspend fun getSellerListings(sellerId: String): Result<List<SellerListing>> {
        delay(500)
        return Result.Success(FakeSellerData.listings.filter {
            it.seller.id == sellerId && it.isActive
        })
    }

    override suspend fun searchListings(query: String): Result<List<SellerListing>> {
        delay(400)
        val lower = query.lowercase()
        val results = FakeSellerData.listings.filter { listing ->
            listing.isActive && (
                listing.drug.name.lowercase().contains(lower) ||
                listing.drug.genericName.lowercase().contains(lower) ||
                listing.drug.category.displayName.lowercase().contains(lower) ||
                listing.seller.businessName.lowercase().contains(lower)
            )
        }
        return Result.Success(results)
    }
}
