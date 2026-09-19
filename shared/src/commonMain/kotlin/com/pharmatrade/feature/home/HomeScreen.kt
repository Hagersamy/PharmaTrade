package com.pharmatrade.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.presentation.cart.PharmacyCartScreen
import com.pharmatrade.feature.pharmacyorder.presentation.home.PharmacyHomeScreen
import com.pharmatrade.feature.pharmacyorder.presentation.home.PharmacyHomeViewModel
import com.pharmatrade.feature.pharmacyorder.presentation.orderlist.OrderListScreen
import com.pharmatrade.feature.pharmacyorder.presentation.orderlist.OrderListViewModel
import com.pharmatrade.feature.seller.presentation.dashboard.SellerDashboardScreen
import com.pharmatrade.feature.seller.presentation.dashboard.SellerDashboardViewModel
import com.pharmatrade.feature.supplierorder.presentation.orderlist.SellerOrdersScreen
import com.pharmatrade.feature.supplierorder.presentation.orderlist.SellerOrdersViewModel

enum class HomeTab { HOME, CART, ORDERS, PROFILE }

@Composable
fun HomeScreen(
    sellerDashboardViewModel: SellerDashboardViewModel,
    pharmacyHomeViewModel: PharmacyHomeViewModel,
    orderListViewModel: OrderListViewModel,
    sellerOrdersViewModel: SellerOrdersViewModel,
    profileViewModel: ProfileViewModel,
    // Hoisted to the caller (rather than kept as internal state) so that a system/predictive
    // back press — handled above this screen, at the nav-graph level — can react to which tab
    // is showing: switch back to the Home tab first, and only pop/exit once already on it.
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    unreadNotificationCount: Int,
    unreadPharmacyOrderIds: Set<String> = emptySet(),
    unreadSupplierOrderIds: Set<String> = emptySet(),
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToUploadInventory: () -> Unit,
    onNavigateToAddListing: () -> Unit,
    onNavigateToEditListing: (String) -> Unit,
    onNavigateToOrderMode: () -> Unit,
    onNavigateToOrderDetail: (String) -> Unit,
    onNavigateToSupplierOrderDetail: (String) -> Unit,
    onCheckoutAll: (orders: List<Pair<String, String>>) -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by SessionManager.currentUser.collectAsStateWithLifecycle()
    val pharmacyHomeState by pharmacyHomeViewModel.uiState.collectAsStateWithLifecycle()
    val isSeller = currentUser?.userType == UserType.SELLER

    Scaffold(
        topBar = {
            HomeTopBar(
                isSeller = isSeller,
                unreadNotificationCount = unreadNotificationCount,
                onNotificationsClick = onNavigateToNotifications,
                onCreateOrder = onNavigateToOrderMode
            )
        },
        bottomBar = {
            HomeBottomBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                isSeller = isSeller,
                ordersBadgeCount = if (isSeller) 0 else pharmacyHomeState.activeOrdersTotal,
                cartBadgeCount = if (isSeller) 0 else pharmacyHomeState.totalCartItems
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                HomeTab.HOME -> {
                    if (isSeller) {
                        SellerDashboardScreen(
                            viewModel = sellerDashboardViewModel,
                            showTopBar = false,
                            onNavigateToUploadInventory = onNavigateToUploadInventory,
                            onNavigateToAddListing = onNavigateToAddListing,
                            onNavigateToEditListing = onNavigateToEditListing,
                            onSearchTap = onNavigateToSearch,
                            onLogout = {}
                        )
                    } else {
                        PharmacyHomeScreen(viewModel = pharmacyHomeViewModel)
                    }
                }
                HomeTab.CART -> PharmacyCartScreen(viewModel = pharmacyHomeViewModel, onCheckoutAll = onCheckoutAll)
                HomeTab.ORDERS -> if (isSeller) {
                    SellerOrdersScreen(
                        viewModel = sellerOrdersViewModel,
                        onOrderClick = onNavigateToSupplierOrderDetail,
                        unreadOrderIds = unreadSupplierOrderIds
                    )
                } else {
                    OrderListScreen(
                        viewModel = orderListViewModel,
                        onOrderClick = onNavigateToOrderDetail,
                        unreadOrderIds = unreadPharmacyOrderIds
                    )
                }
                HomeTab.PROFILE -> ProfileScreen(viewModel = profileViewModel, user = currentUser, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    isSeller: Boolean,
    unreadNotificationCount: Int,
    onNotificationsClick: () -> Unit,
    onCreateOrder: () -> Unit
) {
    Surface(shadowElevation = 4.dp, color = TopBarContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val strings = LocalStrings.current
            Column {
                Text(
                    text = if (isSeller) strings.homeTaglineSeller else strings.homeTaglineBuyer,
                    style = MaterialTheme.typography.labelMedium,
                    color = TopBarContent.copy(alpha = 0.8f)
                )
                Text(
                    text = strings.appTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TopBarContent
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isSeller) {
                    IconButton(onClick = onCreateOrder) {
                        Icon(
                            imageVector = Icons.Filled.AddShoppingCart,
                            contentDescription = strings.homeCreateOrderContentDescription,
                            tint = TopBarContent
                        )
                    }
                }
                IconButton(onClick = onNotificationsClick) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) Badge { Text("$unreadNotificationCount") }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = strings.notificationsTitle,
                            tint = TopBarContent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    isSeller: Boolean,
    ordersBadgeCount: Int,
    cartBadgeCount: Int
) {
    val strings = LocalStrings.current
    NavigationBar(containerColor = SurfaceWhite) {
        NavigationBarItem(
            selected = selectedTab == HomeTab.HOME,
            onClick = { onTabSelected(HomeTab.HOME) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(strings.navHome) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlueContainer)
        )
        if (!isSeller) {
            NavigationBarItem(
                selected = selectedTab == HomeTab.CART,
                onClick = { onTabSelected(HomeTab.CART) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (cartBadgeCount > 0) Badge { Text("$cartBadgeCount") }
                        }
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                    }
                },
                label = { Text(strings.navCart) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlueContainer)
            )
        }
        NavigationBarItem(
            selected = selectedTab == HomeTab.ORDERS,
            onClick = { onTabSelected(HomeTab.ORDERS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (ordersBadgeCount > 0) Badge { Text("$ordersBadgeCount") }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                }
            },
            label = { Text(strings.navOrders) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlueContainer)
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.PROFILE,
            onClick = { onTabSelected(HomeTab.PROFILE) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text(strings.navProfile) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlueContainer)
        )
    }
}
