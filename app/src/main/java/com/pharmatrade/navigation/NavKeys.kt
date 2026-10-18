package com.pharmatrade.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class OrderSupplierPair(val orderId: String, val supplierId: String)

// A draft order line handed from the Cart tab to the supplier page (drafts' lines can't be
// re-read from the backend, so they travel with the key).
@Serializable
data class DraftOrderLine(val id: String, val drugId: String, val drugName: String, val quantity: Int)

@Serializable
sealed class NavKeys : NavKey {
    // Auth
    @Serializable data object Intro : NavKeys()
    @Serializable data object Welcome : NavKeys()
    @Serializable data object Login : NavKeys()
    @Serializable data object Register : NavKeys()

    // Main shell
    @Serializable data object Home : NavKeys()
    @Serializable data object Notifications : NavKeys()
    @Serializable data object Settings : NavKeys()

    // Search
    @Serializable data object BuyerSearch : NavKeys()
    @Serializable data object SellerSearch : NavKeys()

    // Seller sub-screens
    @Serializable data object AddListing : NavKeys()
    @Serializable data class EditListing(val listingId: String) : NavKeys()

    // Buyer sub-screens
    @Serializable data class SellerDrugs(val sellerId: String) : NavKeys()

    // Shared
    @Serializable data object Cart : NavKeys()
    @Serializable data class OrderSuccess(val orderId: String) : NavKeys()

    // Seller tools
    @Serializable data object InventoryUpload : NavKeys()

    // Pharmacy order flow
    @Serializable data object OrderMode : NavKeys()
    @Serializable data class OrderItems(
        val orderId: String,
        val supplierId: String? = null,
        val initialLines: List<DraftOrderLine> = emptyList()
    ) : NavKeys()
    @Serializable data class Allocation(val orderId: String, val supplierId: String? = null) : NavKeys()
    @Serializable data class OrderDetail(val orderId: String) : NavKeys()

    // "Checkout all" — allocates every supplier's draft order in the cart at once, shown
    // together on one summary screen; each order id is paired with its supplier id.
    @Serializable data class Checkout(val orders: List<OrderSupplierPair>) : NavKeys()

    // Pharmacy marketplace (P2P): one pharmacy sells surplus stock to another.
    // mine = the current pharmacy owns it (manage it) rather than browsing it to buy.
    @Serializable data class MarketCollection(val collectionId: String, val mine: Boolean) : NavKeys()
    // asSeller = the current pharmacy is the selling side of the order.
    @Serializable data class MarketOrder(val orderId: String, val asSeller: Boolean) : NavKeys()

    // Supplier order flow (seller side)
    @Serializable data class SupplierOrderDetail(val orderId: String) : NavKeys()

    // Admin
    @Serializable data object AdminDashboard : NavKeys()

    // Post-registration
    @Serializable data object PendingApproval : NavKeys()
    @Serializable data object RegistrationDeclined : NavKeys()
}
