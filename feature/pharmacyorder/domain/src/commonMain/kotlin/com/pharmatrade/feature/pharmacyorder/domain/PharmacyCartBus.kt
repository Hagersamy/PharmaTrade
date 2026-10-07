package com.pharmatrade.feature.pharmacyorder.domain

import com.pharmatrade.feature.pharmacyorder.domain.model.DraftOrderItem
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

// One line of a draft order built on the supplier page, plus the catalog item it maps to on the
// Home cart (null if the supplier page couldn't build one — Home then looks it up itself).
data class SupplierOrderLine(val item: DraftOrderItem, val catalogItem: SupplierCatalogItem?)

// A supplier's draft order as the supplier page (OrderItems) left it. Draft lines can't be
// re-read from the backend (GET pharmacy/orders/{id} returns no items for drafts), so the cart
// takes them from here rather than re-fetching.
data class SupplierPageOrder(
    val orderId: String,
    val supplierId: String,
    val lines: List<SupplierOrderLine>
)

// AllocationViewModel (confirm/cancel a supplier's draft order) runs on a separate nav
// back-stack entry from PharmacyHomeViewModel, with no direct reference back to it. Once a
// draft order is confirmed or cancelled there, it's no longer a valid target for "add item" —
// this bus lets Home know to drop its local cart bookkeeping for that supplier so the next tap
// on "+" starts a fresh draft order instead of reusing a dead one.
object PharmacyCartBus {
    private val _supplierOrderResolved = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val supplierOrderResolved: SharedFlow<String> = _supplierOrderResolved

    fun notifyResolved(supplierId: String) {
        _supplierOrderResolved.tryEmit(supplierId)
    }

    // The supplier page (OrderItems) also lives on its own back-stack entry; when the buyer
    // leaves it, its order lands in (or updates) the Home cart through this.
    private val _supplierPageOrder = MutableSharedFlow<SupplierPageOrder>(extraBufferCapacity = 4)
    val supplierPageOrder: SharedFlow<SupplierPageOrder> = _supplierPageOrder

    fun publishSupplierPageOrder(order: SupplierPageOrder) {
        _supplierPageOrder.tryEmit(order)
    }
}
