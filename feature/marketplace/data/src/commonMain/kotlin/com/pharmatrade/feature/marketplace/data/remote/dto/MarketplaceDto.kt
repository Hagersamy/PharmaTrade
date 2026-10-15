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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
data class FeaturedItemsRequest(@SerialName("item_ids") val itemIds: List<Int>)

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
    @SerialName("pharmacy") val pharmacy: MarketNamedDto? = null,
    // NOTE: assumed names — the docs only show the branch's name. Kept as JsonElement so a number
    // phone or an address object can't fail decoding the whole order.
    @SerialName("phone") @JsonNames("mobile", "phone_number", "contact_phone") val phone: JsonElement? = null,
    @SerialName("address") @JsonNames("full_address", "location", "address_line") val address: JsonElement? = null
) {
    val displayName: String get() = name ?: pharmacy?.name ?: ""
    val phoneText: String? get() = (phone ?: pharmacy?.phone).rawStringOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    val addressText: String? get() = address.addressText() ?: pharmacy?.address.addressText()
}

@Serializable
data class MarketNamedDto(
    @SerialName("name") val name: String? = null,
    @SerialName("phone") @JsonNames("mobile", "phone_number") val phone: JsonElement? = null,
    @SerialName("address") @JsonNames("full_address", "location") val address: JsonElement? = null
)

// An address may come as plain text or as an object ({address, street, area, city: {name}, ...}).
internal fun JsonElement?.addressText(): String? = when (this) {
    is JsonPrimitive -> rawStringOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    is JsonObject -> {
        fun part(key: String): String? = when (val value = this[key]) {
            is JsonPrimitive -> value.rawStringOrNull()
            is JsonObject -> (value["name"] ?: value["name_en"] ?: value["name_ar"]).rawStringOrNull()
            else -> null
        }?.trim()?.takeIf { it.isNotEmpty() }
        part("full_address")
            ?: listOfNotNull(
                part("address") ?: part("address_line"),
                part("building"),
                part("street"),
                part("area") ?: part("district"),
                part("city"),
                part("governorate")
            ).distinct().joinToString(", ").takeIf { it.isNotEmpty() }
    }
    else -> null
}

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
    @SerialName("items_count") @JsonNames("total_items", "drugs_count", "item_count") val itemsCount: JsonElement? = null,
    // null until the seller picks them; cleared again whenever the items are replaced.
    @SerialName("featured_item_ids") val featuredItemIds: List<JsonElement>? = null,
    @SerialName("featured_items") val featuredItems: List<MarketCollectionItemDto>? = null,
    @SerialName("branch") @JsonNames("seller_branch", "pharmacy_branch") val branch: MarketBranchDto? = null,
    @SerialName("seller_name") @JsonNames("branch_name", "pharmacy_name") val sellerName: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toDomain(): MarketCollection {
        val sentFeatured = featuredItems?.map { it.toDomain() }
        // null = the seller hasn't picked yet. Browse then sends EVERY item as featured_items
        // (seen on the dev server 2026-10-16: their line totals add up to total_price), so in that
        // case they're the full list, not picks.
        val pickedIds = featuredItemIds?.mapNotNull { it.rawStringOrNull() }?.toSet()?.takeIf { it.isNotEmpty() }
        val mappedItems = items?.map { it.toDomain() } ?: sentFeatured?.takeIf { pickedIds == null } ?: emptyList()
        val mappedFeatured = when {
            pickedIds == null -> emptyList()
            else -> (sentFeatured ?: mappedItems).filter { it.id in pickedIds }
        }.take(MarketCollection.MAX_FEATURED_ITEMS)
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
            // Browse sends no items_count. Once the seller has picked, only the picks come back, so
            // the count is unknown (0) and the card shows no number rather than a wrong one.
            itemsCount = itemsCount.rawIntOrZero().takeIf { it > 0 } ?: mappedItems.size,
            featuredItems = mappedFeatured,
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
    // JsonElement so an unexpected shape (e.g. an object) can't break decoding the whole order.
    @SerialName("collector_address") val collectorAddress: JsonElement? = null,
    // NOTE: assumed — flat buyer contact fields, in case they aren't nested under buyer_branch.
    @SerialName("buyer_phone") val buyerPhone: JsonElement? = null,
    @SerialName("buyer_address") val buyerAddress: JsonElement? = null,
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
            buyerPhone = buyerPhone.rawStringOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: buyerBranch?.phoneText,
            buyerAddress = buyerAddress.addressText() ?: buyerBranch?.addressText,
            sellerName = sellerBranch?.displayName?.takeIf { it.isNotEmpty() } ?: mappedCollection?.sellerName.orEmpty(),
            sellerPhone = sellerBranch?.phoneText,
            sellerAddress = sellerBranch?.addressText,
            totalAmount = total,
            commissionPct = pct,
            commissionAmount = commission,
            netAmount = netAmount?.rawDoubleOrZero() ?: (total - commission),
            collectorName = collectorName?.takeIf { it.isNotBlank() },
            collectorPhone = collectorPhone?.takeIf { it.isNotBlank() },
            collectorAddress = collectorAddress.addressText(),
            createdAt = createdAt.orEmpty(),
            acceptedAt = acceptedAt,
            readyAt = readyAt,
            completedAt = completedAt,
            items = mappedCollection?.items ?: emptyList()
        )
    }
}
