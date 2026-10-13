@file:OptIn(ExperimentalSerializationApi::class)

package com.pharmatrade.feature.marketplace.data.remote.dto

import com.pharmatrade.core.network.dto.rawDoubleOrZero
import com.pharmatrade.core.network.dto.rawIntOrZero
import com.pharmatrade.core.network.dto.rawStringOrNull
import com.pharmatrade.feature.marketplace.domain.model.CollectionStatus
import com.pharmatrade.feature.marketplace.domain.model.MarketCollection
import com.pharmatrade.feature.marketplace.domain.model.MarketCollectionItem
import com.pharmatrade.feature.marketplace.domain.model.MarketInventoryItem
import com.pharmatrade.feature.marketplace.domain.model.MarketOrder
import com.pharmatrade.feature.marketplace.domain.model.MarketOrderStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

// NOTE: only the request bodies and a handful of response fields (id, status, items,
// order_number, commission_pct, collector_name, *_at timestamps) are confirmed by the backend's
// Postman suite. Everything else here — how the drug name, branch names and money totals are
// exposed — is a best guess, so each field accepts the likely alternative names via @JsonNames.

// ── Requests ─────────────────────────────────────────────────────────────────

@Serializable
data class AddInventoryRequest(
    @SerialName("drug_id") val drugId: Int,
    @SerialName("quantity_available") val quantityAvailable: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("expiry_date") val expiryDate: String,
    @SerialName("batch_number") val batchNumber: String? = null
)

@Serializable
data class CreateCollectionRequest(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("notes") val notes: String? = null
)

@Serializable
data class CollectionItemsRequest(@SerialName("items") val items: List<CollectionItemRequest>)

@Serializable
data class CollectionItemRequest(
    @SerialName("inventory_id") val inventoryId: Int,
    @SerialName("quantity") val quantity: Int
)

@Serializable
data class PlaceOrderRequest(@SerialName("collection_id") val collectionId: Int)

@Serializable
data class SetCollectorRequest(
    @SerialName("collector_name") val collectorName: String,
    @SerialName("collector_phone") val collectorPhone: String
)

// ── Shared pieces ────────────────────────────────────────────────────────────

@Serializable
data class MarketDrugDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") @JsonNames("trade_name", "drug_name", "drug_name_raw") val name: String? = null,
    @SerialName("scientific_name") val scientificName: String? = null
)

@Serializable
data class MarketBranchDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") @JsonNames("branch_name", "pharmacy_name") val name: String? = null,
    @SerialName("pharmacy") val pharmacy: MarketNamedDto? = null
) {
    val displayName: String get() = name ?: pharmacy?.name ?: ""
}

@Serializable
data class MarketNamedDto(@SerialName("name") val name: String? = null)

// ── Inventory ────────────────────────────────────────────────────────────────

@Serializable
data class MarketInventoryDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("drug_id") val drugId: JsonElement? = null,
    @SerialName("drug_name") @JsonNames("trade_name", "drug_name_raw") val drugName: String? = null,
    @SerialName("drug") val drug: MarketDrugDto? = null,
    @SerialName("quantity_available") @JsonNames("quantity") val quantityAvailable: JsonElement? = null,
    @SerialName("unit_price") val unitPrice: JsonElement? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("batch_number") val batchNumber: String? = null,
    @SerialName("is_active") val isActive: JsonElement? = null
) {
    fun toDomain() = MarketInventoryItem(
        id = id.rawStringOrNull().orEmpty(),
        drugId = (drugId ?: drug?.id).rawStringOrNull().orEmpty(),
        drugName = drugName ?: drug?.name ?: drug?.scientificName ?: "",
        quantityAvailable = quantityAvailable.rawIntOrZero(),
        unitPrice = unitPrice.rawDoubleOrZero(),
        expiryDate = expiryDate?.substringBefore('T').orEmpty(),
        batchNumber = batchNumber,
        // Booleans may come back as true/false or 1/0.
        isActive = isActive.rawStringOrNull()?.let { it == "true" || it == "1" } ?: true
    )
}

// ── Collections ──────────────────────────────────────────────────────────────

@Serializable
data class MarketCollectionItemDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("inventory_id") val inventoryId: JsonElement? = null,
    @SerialName("quantity") val quantity: JsonElement? = null,
    @SerialName("unit_price_snapshot") @JsonNames("unit_price") val unitPrice: JsonElement? = null,
    @SerialName("drug_name") @JsonNames("trade_name") val drugName: String? = null,
    @SerialName("drug") val drug: MarketDrugDto? = null,
    @SerialName("inventory") val inventory: MarketInventoryDto? = null,
    @SerialName("expiry_date") val expiryDate: String? = null
) {
    fun toDomain() = MarketCollectionItem(
        id = id.rawStringOrNull().orEmpty(),
        inventoryId = (inventoryId ?: inventory?.id).rawStringOrNull().orEmpty(),
        drugName = drugName ?: drug?.name ?: inventory?.drugName ?: inventory?.drug?.name ?: "",
        quantity = quantity.rawIntOrZero(),
        unitPrice = unitPrice?.rawDoubleOrZero() ?: inventory?.unitPrice.rawDoubleOrZero(),
        expiryDate = (expiryDate ?: inventory?.expiryDate)?.substringBefore('T')
    )
}

@Serializable
data class MarketCollectionDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("total_price") val totalPrice: JsonElement? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("items") val items: List<MarketCollectionItemDto>? = null,
    @SerialName("items_count") val itemsCount: JsonElement? = null,
    @SerialName("branch") @JsonNames("seller_branch", "pharmacy_branch") val branch: MarketBranchDto? = null,
    @SerialName("seller_name") @JsonNames("branch_name", "pharmacy_name") val sellerName: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toDomain(): MarketCollection {
        val mappedItems = items?.map { it.toDomain() } ?: emptyList()
        return MarketCollection(
            id = id.rawStringOrNull().orEmpty(),
            name = name.orEmpty(),
            description = description.orEmpty(),
            notes = notes.orEmpty(),
            // No seller-set bundle price: the collection costs what its items cost.
            totalPrice = totalPrice?.rawDoubleOrZero()?.takeIf { it > 0 } ?: mappedItems.sumOf { it.lineTotal },
            status = CollectionStatus.from(status),
            sellerName = sellerName ?: branch?.displayName.orEmpty(),
            items = mappedItems,
            itemsCount = itemsCount?.rawIntOrZero() ?: mappedItems.size,
            createdAt = createdAt.orEmpty()
        )
    }
}

// ── Orders ───────────────────────────────────────────────────────────────────

@Serializable
data class MarketOrderDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("collection_id") val collectionId: JsonElement? = null,
    @SerialName("collection") val collection: MarketCollectionDto? = null,
    @SerialName("buyer_branch") @JsonNames("buyer") val buyerBranch: MarketBranchDto? = null,
    @SerialName("seller_branch") @JsonNames("seller") val sellerBranch: MarketBranchDto? = null,
    @SerialName("total_amount") @JsonNames("total_price", "amount") val totalAmount: JsonElement? = null,
    @SerialName("commission_pct") val commissionPct: JsonElement? = null,
    @SerialName("commission_amount") val commissionAmount: JsonElement? = null,
    @SerialName("net_amount") @JsonNames("seller_net_amount", "seller_net") val netAmount: JsonElement? = null,
    @SerialName("collector_name") val collectorName: String? = null,
    @SerialName("collector_phone") val collectorPhone: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("ready_at") val readyAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null
) {
    fun toDomain(): MarketOrder {
        val mappedCollection = collection?.toDomain()
        val total = totalAmount?.rawDoubleOrZero() ?: mappedCollection?.totalPrice ?: 0.0
        val pct = commissionPct.rawDoubleOrZero()
        val commission = commissionAmount?.rawDoubleOrZero() ?: (total * pct / 100.0)
        return MarketOrder(
            id = id.rawStringOrNull().orEmpty(),
            orderNumber = orderNumber ?: id.rawStringOrNull().orEmpty(),
            status = MarketOrderStatus.from(status),
            collectionId = (collectionId ?: collection?.id).rawStringOrNull().orEmpty(),
            collectionName = mappedCollection?.name.orEmpty(),
            buyerName = buyerBranch?.displayName.orEmpty(),
            sellerName = sellerBranch?.displayName?.takeIf { it.isNotEmpty() } ?: mappedCollection?.sellerName.orEmpty(),
            totalAmount = total,
            commissionPct = pct,
            commissionAmount = commission,
            netAmount = netAmount?.rawDoubleOrZero() ?: (total - commission),
            collectorName = collectorName?.takeIf { it.isNotBlank() },
            collectorPhone = collectorPhone?.takeIf { it.isNotBlank() },
            createdAt = createdAt.orEmpty(),
            acceptedAt = acceptedAt,
            readyAt = readyAt,
            completedAt = completedAt,
            items = mappedCollection?.items ?: emptyList()
        )
    }
}
