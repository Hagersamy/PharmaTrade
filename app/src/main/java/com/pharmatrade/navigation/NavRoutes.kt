package com.pharmatrade.navigation

sealed class NavRoutes(val route: String) {
    // Auth
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")

    // Main shell
    object Home : NavRoutes("home")
    object Notifications : NavRoutes("notifications")

    // Search
    object BuyerSearch : NavRoutes("buyer_search")
    object SellerSearch : NavRoutes("seller_search")

    // Seller sub-screens
    object AddListing : NavRoutes("add_listing")
    object EditListing : NavRoutes("edit_listing/{listingId}") {
        fun createRoute(listingId: String) = "edit_listing/$listingId"
    }

    // Buyer sub-screens
    object SellerDrugs : NavRoutes("seller_drugs/{sellerId}") {
        fun createRoute(sellerId: String) = "seller_drugs/$sellerId"
    }

    // Shared
    object Cart : NavRoutes("cart")
    object OrderSuccess : NavRoutes("order_success/{orderId}") {
        fun createRoute(orderId: String) = "order_success/$orderId"
    }

    // Seller tools
    object InventoryUpload : NavRoutes("inventory_upload")

    // Pharmacy order flow
    object OrderMode : NavRoutes("order_mode")
    object OrderItems : NavRoutes("order_items/{orderId}?supplierId={supplierId}") {
        fun createRoute(orderId: String, supplierId: String?) =
            "order_items/$orderId?supplierId=${supplierId ?: ""}"
    }
    object Allocation : NavRoutes("allocation/{orderId}?supplierId={supplierId}") {
        fun createRoute(orderId: String, supplierId: String?) =
            "allocation/$orderId?supplierId=${supplierId ?: ""}"
    }
    object OrderDetail : NavRoutes("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }

    // "Checkout all" — allocates every supplier's draft order in the cart at once, shown
    // together on one summary screen; each order id is paired with its supplier id, encoded as
    // "orderId~supplierId" entries joined by commas (ids are plain alphanumeric, safe unencoded).
    object Checkout : NavRoutes("checkout/{orders}") {
        fun createRoute(orders: List<Pair<String, String>>): String =
            "checkout/" + orders.joinToString(",") { (orderId, supplierId) -> "$orderId~$supplierId" }

        fun parse(encoded: String): List<Pair<String, String>> =
            encoded.split(",")
                .mapNotNull { entry ->
                    val parts = entry.split("~")
                    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) parts[0] to parts[1] else null
                }
    }

    // Supplier order flow (seller side)
    object SupplierOrderDetail : NavRoutes("supplier_order_detail/{orderId}") {
        fun createRoute(orderId: String) = "supplier_order_detail/$orderId"
    }

    // Admin
    object AdminDashboard : NavRoutes("admin_dashboard")

    // Post-registration
    object PendingApproval : NavRoutes("pending_approval")
}
