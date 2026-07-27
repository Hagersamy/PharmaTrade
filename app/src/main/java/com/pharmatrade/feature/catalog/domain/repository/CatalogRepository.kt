package com.pharmatrade.feature.catalog.domain.repository

import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result

interface CatalogRepository {
    suspend fun getAllSellers(): Result<List<Seller>>
    suspend fun getSellerListings(sellerId: String): Result<List<SellerListing>>
    suspend fun searchListings(query: String): Result<List<SellerListing>>
}
