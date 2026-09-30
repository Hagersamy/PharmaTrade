package com.pharmatrade.feature.catalog.domain.usecase

import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.catalog.domain.repository.CatalogRepository

class GetAllSellersUseCase(private val repository: CatalogRepository) {
    suspend operator fun invoke(): Result<List<Seller>> = repository.getAllSellers()
}

class GetSellerListingsUseCase(private val repository: CatalogRepository) {
    suspend operator fun invoke(sellerId: String): Result<List<SellerListing>> =
        repository.getSellerListings(sellerId)
}

class SearchListingsUseCase(private val repository: CatalogRepository) {
    suspend operator fun invoke(query: String): Result<List<SellerListing>> {
        if (query.isBlank()) return Result.Success(emptyList())
        return repository.searchListings(query.trim())
    }
}
