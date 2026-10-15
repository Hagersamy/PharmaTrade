package com.pharmatrade.feature.marketplace.domain.usecase

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.marketplace.domain.model.CollectionItemInput
import com.pharmatrade.feature.marketplace.domain.model.MarketCollection
import com.pharmatrade.feature.marketplace.domain.model.MarketInventoryItem
import com.pharmatrade.feature.marketplace.domain.model.MarketOrder
import com.pharmatrade.feature.marketplace.domain.model.MarketPage
import com.pharmatrade.feature.marketplace.domain.model.SellerOrderAction
import com.pharmatrade.feature.marketplace.domain.repository.MarketplaceRepository

// ── Selling ─────────────────────────────────────────────────────────────────

class GetMarketInventoryUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(): Result<List<MarketInventoryItem>> = repository.getInventory()
}

class AddMarketInventoryItemUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(
        drugId: String,
        quantity: Int,
        unitPrice: Double,
        expiryDate: String,
        batchNumber: String?
    ): Result<MarketInventoryItem> {
        validateInventoryInput(drugId, quantity, unitPrice, expiryDate)?.let { return it }
        return repository.addInventoryItem(drugId, quantity, unitPrice, expiryDate, batchNumber?.takeIf { it.isNotBlank() })
    }
}

class UpdateMarketInventoryItemUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(
        inventoryId: String,
        drugId: String,
        quantity: Int,
        unitPrice: Double,
        expiryDate: String,
        batchNumber: String?
    ): Result<MarketInventoryItem> {
        validateInventoryInput(drugId, quantity, unitPrice, expiryDate)?.let { return it }
        return repository.updateInventoryItem(
            inventoryId, drugId, quantity, unitPrice, expiryDate, batchNumber?.takeIf { it.isNotBlank() }
        )
    }
}

private fun validateInventoryInput(drugId: String, quantity: Int, unitPrice: Double, expiryDate: String): Result.Error? = when {
    drugId.isBlank() -> Result.Error("Select a drug")
    quantity <= 0 -> Result.Error("Quantity must be greater than 0")
    unitPrice <= 0 -> Result.Error("Price must be greater than 0")
    expiryDate.isBlank() -> Result.Error("Expiry date is required")
    else -> null
}

class ImportMarketInventoryUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(fileUri: String, fileName: String): Result<Unit> =
        repository.importInventory(fileUri, fileName)
}

class GetMyCollectionsUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(): Result<List<MarketCollection>> = repository.getMyCollections()
}

class GetMyCollectionUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(collectionId: String): Result<MarketCollection> = repository.getMyCollection(collectionId)
}

class CreateCollectionUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(name: String, description: String?, notes: String?): Result<MarketCollection> {
        if (name.isBlank()) return Result.Error("Name cannot be empty")
        return repository.createCollection(
            name = name.trim(),
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() }
        )
    }
}

class SetCollectionItemsUseCase(private val repository: MarketplaceRepository) {
    // Replaces every item in the draft collection with `items`.
    suspend operator fun invoke(collectionId: String, items: List<CollectionItemInput>): Result<MarketCollection> {
        val nonEmpty = items.filter { it.quantity > 0 }
        if (nonEmpty.isEmpty()) return Result.Error("Quantity must be greater than 0")
        return repository.setCollectionItems(collectionId, nonEmpty)
    }
}

class PublishCollectionUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(collectionId: String): Result<MarketCollection> = repository.publishCollection(collectionId)
}

class SetFeaturedItemsUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(collectionId: String, itemIds: List<String>): Result<MarketCollection> {
        val ids = itemIds.distinct()
        if (ids.isEmpty()) return Result.Error("Select at least one drug")
        if (ids.size > MarketCollection.MAX_FEATURED_ITEMS) {
            return Result.Error("Select up to ${MarketCollection.MAX_FEATURED_ITEMS} drugs")
        }
        return repository.setFeaturedItems(collectionId, ids)
    }
}

class DeactivateCollectionUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(collectionId: String): Result<MarketCollection> = repository.deactivateCollection(collectionId)
}

class GetMarketSalesUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(): Result<List<MarketOrder>> = repository.getSales()
}

class SellerMarketOrderActionUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(orderId: String, action: SellerOrderAction): Result<MarketOrder> =
        repository.sellerOrderAction(orderId, action)
}

// ── Buying ──────────────────────────────────────────────────────────────────

class BrowseCollectionsUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(page: Int = 1): Result<MarketPage<MarketCollection>> = repository.browse(page)
}

class GetBrowseCollectionUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(collectionId: String): Result<MarketCollection> = repository.getBrowseCollection(collectionId)
}

class PlaceMarketOrderUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(collectionId: String): Result<MarketOrder> = repository.placeOrder(collectionId)
}

class SetCollectorUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(orderId: String, name: String, phone: String): Result<MarketOrder> {
        if (name.isBlank()) return Result.Error("Name cannot be empty")
        if (phone.isBlank()) return Result.Error("Phone number cannot be empty")
        return repository.setCollector(orderId, name.trim(), phone.trim())
    }
}

class CancelMarketPurchaseUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(orderId: String): Result<Unit> = repository.cancelPurchase(orderId)
}

class GetMarketPurchasesUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(): Result<List<MarketOrder>> = repository.getPurchases()
}

class GetMarketOrderUseCase(private val repository: MarketplaceRepository) {
    suspend operator fun invoke(orderId: String, asSeller: Boolean): Result<MarketOrder> =
        repository.getOrder(orderId, asSeller)
}
