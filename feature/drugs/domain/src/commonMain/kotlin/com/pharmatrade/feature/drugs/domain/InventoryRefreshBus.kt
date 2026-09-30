package com.pharmatrade.feature.drugs.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

// InventoryUploadViewModel and SellerDashboardViewModel are scoped to different nav back-stack
// entries with no direct reference to each other. This bus lets the upload flow announce
// "inventory changed on the backend" so Home can refresh immediately instead of waiting on a
// lifecycle event (screen resume) that may lag behind or get missed.
object InventoryRefreshBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events

    fun notifyChanged() {
        _events.tryEmit(Unit)
    }

    // The Add Drug Listing form pops itself right after a successful save, so it can't show its
    // own confirmation — the dashboard underneath it (alive on the back stack) shows it instead.
    private val _listingAdded = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val listingAdded: SharedFlow<String> = _listingAdded

    fun notifyListingAdded(drugName: String) {
        _listingAdded.tryEmit(drugName)
    }
}
