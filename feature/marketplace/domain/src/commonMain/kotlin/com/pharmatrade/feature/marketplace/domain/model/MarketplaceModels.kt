package com.pharmatrade.feature.marketplace.domain.model

// P2P pharmacy marketplace: a pharmacy lists surplus stock (its marketplace inventory), bundles
// some of it into a collection, and publishes it; another pharmacy in an overlapping zone orders
// the whole collection and sends a collector to pick it up.

data class MarketInventoryItem(
    val id: String,
    val drugId: String,
    val drugName: String,
    val quantityAvailable: Int,
    val unitPrice: Double,
    val expiryDate: String,
    val batchNumber: String?,
    val isActive: Boolean
)

data class MarketCollectionItem(
    val id: String,
    val inventoryId: String,
    val drugName: String,
    val quantity: Int,
    // Price locked when the item was added to the collection (unit_price_snapshot).
    val unitPrice: Double,
    val expiryDate: String?
) {
    val lineTotal: Double get() = unitPrice * quantity
}

enum class CollectionStatus(val apiValue: String) {
    DRAFT("draft"),
    ACTIVE("active"),
    ORDERED("ordered"),
    COMPLETED("completed"),
    SUSPENDED("suspended"),
    UNKNOWN("");

    companion object {
        fun from(value: String?): CollectionStatus =
            entries.firstOrNull { it != UNKNOWN && it.apiValue == value?.lowercase() } ?: UNKNOWN
    }
}

data class MarketCollection(
    val id: String,
    val name: String,
    val description: String,
    val notes: String,
    // Sum of the items at their locked prices.
    val totalPrice: Double,
    val status: CollectionStatus,
    val sellerName: String,
    val items: List<MarketCollectionItem>,
    val itemsCount: Int,
    val createdAt: String
)

enum class MarketOrderStatus(val apiValue: String) {
    PENDING_COLLECTOR("pending_collector"),
    PENDING_SELLER("pending_seller"),
    ACCEPTED("accepted"),
    READY_FOR_PICKUP("ready_for_pickup"),
    COMPLETED("completed"),
    REJECTED("rejected"),
    CANCELLED("cancelled"),
    UNKNOWN("");

    val isTerminal: Boolean get() = this == COMPLETED || this == REJECTED || this == CANCELLED

    // Buyer: may cancel until the seller has accepted.
    val buyerCanCancel: Boolean get() = this == PENDING_COLLECTOR || this == PENDING_SELLER

    // Seller: may cancel an accepted order until it's marked ready.
    val sellerCanCancel: Boolean get() = this == ACCEPTED

    companion object {
        fun from(value: String?): MarketOrderStatus =
            entries.firstOrNull { it != UNKNOWN && it.apiValue == value?.lowercase() } ?: UNKNOWN
    }
}

data class MarketOrder(
    val id: String,
    val orderNumber: String,
    val status: MarketOrderStatus,
    val collectionId: String,
    val collectionName: String,
    val buyerName: String,
    val sellerName: String,
    val totalAmount: Double,
    val commissionPct: Double,
    val commissionAmount: Double,
    // What the seller receives after the platform commission.
    val netAmount: Double,
    val collectorName: String?,
    val collectorPhone: String?,
    // Where the collector picks the order up; sent by the backend once the order is accepted.
    val collectorAddress: String?,
    val createdAt: String,
    val acceptedAt: String?,
    val readyAt: String?,
    val completedAt: String?,
    val items: List<MarketCollectionItem>
)

data class MarketPage<T>(
    val items: List<T>,
    val currentPage: Int,
    val lastPage: Int
) {
    val hasNextPage: Boolean get() = currentPage < lastPage
}

// The seller-side order transitions (POST pharmacy/marketplace/seller/orders/{id}/{path}).
enum class SellerOrderAction(val path: String) {
    ACCEPT("accept"),
    REJECT("reject"),
    READY("ready"),
    COMPLETE("complete"),
    CANCEL("cancel")
}

data class CollectionItemInput(val inventoryId: String, val quantity: Int)
