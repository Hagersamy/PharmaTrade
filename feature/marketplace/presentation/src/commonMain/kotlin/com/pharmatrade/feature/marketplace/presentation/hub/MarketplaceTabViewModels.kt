package com.pharmatrade.feature.marketplace.presentation.hub

// The marketplace tab's per-sub-tab ViewModels, bundled so the Home shell can host them as one.
class MarketplaceTabViewModels(
    val browse: MarketBrowseViewModel,
    val purchases: MarketOrdersViewModel,
    val collections: MyCollectionsViewModel,
    val inventory: MarketInventoryViewModel,
    val sales: MarketOrdersViewModel
) {
    fun refresh(tab: MarketTab) = when (tab) {
        MarketTab.BROWSE -> browse.refresh()
        MarketTab.PURCHASES -> purchases.refresh()
        MarketTab.COLLECTIONS -> collections.refresh()
        MarketTab.INVENTORY -> inventory.refresh()
        MarketTab.SALES -> sales.refresh()
    }
}
