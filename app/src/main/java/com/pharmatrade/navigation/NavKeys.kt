package com.pharmatrade.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class OrderSupplierPair(val orderId: String, val supplierId: String)

@Serializable
sealed class NavKeys : NavKey {
    // Auth
    @Serializable data object Login : NavKeys()
    @Serializable data object Register : NavKeys()

    // Main shell
    @Serializable data object Home : NavKeys()
    @Serializable data object Notifications : NavKeys()

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
    @Serializable data class OrderItems(val orderId: String, val supplierId: String? = null) : NavKeys()
    @Serializable data class Allocation(val orderId: String, val supplierId: String? = null) : NavKeys()
    @Serializable data class OrderDetail(val orderId: String) : NavKeys()

    // "Checkout all" — allocates every supplier's draft order in the cart at once, shown
    // together on one summary screen; each order id is paired with its supplier id.
    @Serializable data class Checkout(val orders: List<OrderSupplierPair>) : NavKeys()

    // Supplier order flow (seller side)
    @Serializable data class SupplierOrderDetail(val orderId: String) : NavKeys()

    // Admin
    @Serializable data object AdminDashboard : NavKeys()

    // Post-registration
    @Serializable data object PendingApproval : NavKeys()
}
