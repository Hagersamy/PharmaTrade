package com.pharmatrade.feature.pharmacyorder.domain.repository

import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem

// The Home cart, kept on the device so it survives the app being killed before checkout.
// The backend can't rebuild it: draft orders come back from GET pharmacy/orders (status=draft)
// and GET pharmacy/orders/{id} with no items and no supplier.
interface PharmacyCartStore {
    fun load(ownerId: String): PharmacyCartSnapshot?
    fun save(ownerId: String, snapshot: PharmacyCartSnapshot)
}

data class PharmacyCartSnapshot(
    // supplierId -> the draft order that supplier's cart lines live in.
    val orderIdsBySupplier: Map<String, String>,
    val lines: List<PharmacyCartLine>
)

data class PharmacyCartLine(
    val item: SupplierCatalogItem,
    // What the buyer asked for, and what the backend draft order holds (they differ while a
    // +/- change is still waiting to be sent).
    val quantity: Int,
    val syncedQuantity: Int,
    // The draft-order line backing syncedQuantity, needed to remove it on the next change.
    val backendItemId: String?
)
