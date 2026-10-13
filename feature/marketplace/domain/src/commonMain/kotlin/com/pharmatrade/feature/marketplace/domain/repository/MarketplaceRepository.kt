package com.pharmatrade.feature.marketplace.domain.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.marketplace.domain.model.CollectionItemInput
import com.pharmatrade.feature.marketplace.domain.model.MarketCollection
import com.pharmatrade.feature.marketplace.domain.model.MarketInventoryItem
import com.pharmatrade.feature.marketplace.domain.model.MarketOrder
import com.pharmatrade.feature.marketplace.domain.model.MarketPage
import com.pharmatrade.feature.marketplace.domain.model.SellerOrderAction

interface MarketplaceRepository {
    // ── Selling: inventory ──
    suspend fun getInventory(): Result<List<MarketInventoryItem>>
    suspend fun addInventoryItem(
        drugId: String,
        quantity: Int,
        unitPrice: Double,
        expiryDate: String,
        batchNumber: String?
    ): Result<MarketInventoryItem>
    suspend fun importInventory(fileUri: String, fileName: String): Result<Unit>

    // ── Selling: collections ──
    suspend fun getMyCollections(): Result<List<MarketCollection>>
    suspend fun getMyCollection(collectionId: String): Result<MarketCollection>
    suspend fun createCollection(
        name: String,
        description: String?,
        notes: String?
    ): Result<MarketCollection>
    suspend fun setCollectionItems(collectionId: String, items: List<CollectionItemInput>): Result<MarketCollection>
    suspend fun publishCollection(collectionId: String): Result<MarketCollection>
    suspend fun deactivateCollection(collectionId: String): Result<MarketCollection>

    // ── Selling: incoming orders ──
    suspend fun getSales(): Result<List<MarketOrder>>
    suspend fun sellerOrderAction(orderId: String, action: SellerOrderAction): Result<MarketOrder>

    // ── Buying ──
    suspend fun browse(page: Int): Result<MarketPage<MarketCollection>>
    suspend fun getBrowseCollection(collectionId: String): Result<MarketCollection>
    suspend fun placeOrder(collectionId: String): Result<MarketOrder>
    suspend fun setCollector(orderId: String, name: String, phone: String): Result<MarketOrder>
    suspend fun cancelPurchase(orderId: String): Result<Unit>
    suspend fun getPurchases(): Result<List<MarketOrder>>

    // Either side's order — asSeller picks the seller/orders endpoints.
    suspend fun getOrder(orderId: String, asSeller: Boolean): Result<MarketOrder>
}
