package com.pharmatrade.feature.pharmacyorder.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

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
}
