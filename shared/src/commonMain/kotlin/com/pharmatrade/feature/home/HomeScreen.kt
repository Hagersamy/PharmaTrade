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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.core.ui.components.GlassBarItem
import com.pharmatrade.core.ui.components.LiquidGlassBottomBar
import com.pharmatrade.core.ui.components.LocalBottomBarInset
import com.pharmatrade.core.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.pharmatrade.feature.pharmacyorder.domain.model.DraftOrderItem
import com.pharmatrade.feature.pharmacyorder.presentation.cart.PharmacyCartScreen
import com.pharmatrade.feature.pharmacyorder.presentation.home.PharmacyHomeScreen
import com.pharmatrade.feature.pharmacyorder.presentation.home.PharmacySearchScreen
import com.pharmatrade.feature.pharmacyorder.presentation.home.PharmacyHomeViewModel
import com.pharmatrade.feature.pharmacyorder.presentation.orderlist.OrderListScreen
import com.pharmatrade.feature.pharmacyorder.presentation.orderlist.OrderListViewModel
import com.pharmatrade.feature.profile.presentation.ProfileScreen
import com.pharmatrade.feature.profile.presentation.ProfileViewModel
import com.pharmatrade.feature.seller.presentation.dashboard.SellerDashboardScreen
import com.pharmatrade.feature.seller.presentation.dashboard.SellerDashboardViewModel
import com.pharmatrade.feature.supplierorder.presentation.orderlist.SellerOrdersScreen
import com.pharmatrade.feature.supplierorder.presentation.orderlist.SellerOrdersViewModel

enum class HomeTab { HOME, CART, ORDERS, PROFILE }

@Composable
fun HomeScreen(
    // Role-specific ViewModels are null for the other role: each one fetches its data as soon
    // as it's created, so building all of them made a supplier hit pharmacy/* endpoints (and a
    // pharmacy hit supplier/*), which the backend rejects with 403.
    sellerDashboardViewModel: SellerDashboardViewModel?,
    pharmacyHomeViewModel: PharmacyHomeViewModel?,
    orderListViewModel: OrderListViewModel?,
    sellerOrdersViewModel: SellerOrdersViewModel?,
    profileViewModel: ProfileViewModel,
    // Hoisted to the caller (rather than kept as internal state) so that a system/predictive
    // back press — handled above this screen, at the nav-graph level — can react to which tab
    // is showing: switch back to the Home tab first, and only pop/exit once already on it.
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    // Hoisted for the same reason: system back closes the search screen before anything else.
    isPharmacySearchOpen: Boolean,
    onPharmacySearchOpenChange: (Boolean) -> Unit,
    unreadNotificationCount: Int,
    unreadPharmacyOrderIds: Set<String> = emptySet(),
    unreadSupplierOrderIds: Set<String> = emptySet(),
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToUploadInventory: () -> Unit,
    onNavigateToAddListing: () -> Unit,
    onNavigateToEditListing: (String) -> Unit,
    onNavigateToOrderMode: () -> Unit,
    onNavigateToOrderDetail: (String) -> Unit,
    onNavigateToSupplierOrderDetail: (String) -> Unit,
    onCheckoutAll: (orders: List<Pair<String, String>>) -> Unit,
    onOpenSupplierOrder: (orderId: String, supplierId: String, lines: List<DraftOrderItem>) -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by SessionManager.currentUser.collectAsStateWithLifecycle()
    val pharmacyHomeState = pharmacyHomeViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val isSeller = currentUser?.userType == UserType.SELLER

    // The pharmacy drug search covers the whole of Home (top bar and bottom bar included) — see
    // PharmacySearchScreen for why it isn't a separate nav destination.
    if (isPharmacySearchOpen && pharmacyHomeViewModel != null) {
        PharmacySearchScreen(
            viewModel = pharmacyHomeViewModel,
            onBack = { onPharmacySearchOpenChange(false) }
        )
        return
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                isSeller = isSeller,
                userName = currentUser?.name.orEmpty(),
                unreadNotificationCount = unreadNotificationCount,
                onNotificationsClick = onNavigateToNotifications,
                onCreateOrder = onNavigateToOrderMode,
                onSearchClick = { onPharmacySearchOpenChange(true) }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        // The glass bar floats over the tab content (rather than sitting in Scaffold's bottomBar
        // slot) so content scrolls behind it and shows through the blur. Its measured height is
        // published as LocalBottomBarInset so lists can pad their last item clear of it.
        val hazeState = remember { HazeState() }
        val density = LocalDensity.current
        var bottomBarHeight by remember { mutableStateOf(0.dp) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CompositionLocalProvider(LocalBottomBarInset provides bottomBarHeight) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(hazeState)
                ) {
                    HomeTabContent(
                        selectedTab = selectedTab,
                        sellerDashboardViewModel = sellerDashboardViewModel,
                        pharmacyHomeViewModel = pharmacyHomeViewModel,
                        orderListViewModel = orderListViewModel,
                        sellerOrdersViewModel = sellerOrdersViewModel,
                        profileViewModel = profileViewModel,
                        currentUser = currentUser,
                        unreadPharmacyOrderIds = unreadPharmacyOrderIds,
                        unreadSupplierOrderIds = unreadSupplierOrderIds,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToUploadInventory = onNavigateToUploadInventory,
                        onNavigateToAddListing = onNavigateToAddListing,
                        onNavigateToEditListing = onNavigateToEditListing,
                        onNavigateToOrderDetail = onNavigateToOrderDetail,
                        onNavigateToSupplierOrderDetail = onNavigateToSupplierOrderDetail,
                        onCheckoutAll = onCheckoutAll,
                        onOpenSupplierOrder = onOpenSupplierOrder,
                        onLogout = onLogout
                    )
                }
            }
            HomeBottomBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                isSeller = isSeller,
                ordersBadgeCount = pharmacyHomeState?.activeOrdersTotal ?: 0,
                cartBadgeCount = pharmacyHomeState?.totalCartItems ?: 0,
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomBarHeight = with(density) { it.height.toDp() } }
            )
        }
    }
}

@Composable
private fun HomeTabContent(
    selectedTab: HomeTab,
    sellerDashboardViewModel: SellerDashboardViewModel?,
    pharmacyHomeViewModel: PharmacyHomeViewModel?,
    orderListViewModel: OrderListViewModel?,
    sellerOrdersViewModel: SellerOrdersViewModel?,
    profileViewModel: ProfileViewModel,
    currentUser: User?,
    unreadPharmacyOrderIds: Set<String>,
    unreadSupplierOrderIds: Set<String>,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToUploadInventory: () -> Unit,
    onNavigateToAddListing: () -> Unit,
    onNavigateToEditListing: (String) -> Unit,
    onNavigateToOrderDetail: (String) -> Unit,
    onNavigateToSupplierOrderDetail: (String) -> Unit,
    onCheckoutAll: (orders: List<Pair<String, String>>) -> Unit,
    onOpenSupplierOrder: (orderId: String, supplierId: String, lines: List<DraftOrderItem>) -> Unit,
    onLogout: () -> Unit
) {
    when (selectedTab) {
        HomeTab.HOME -> {
            if (sellerDashboardViewModel != null) {
                SellerDashboardScreen(
                    viewModel = sellerDashboardViewModel,
                    showTopBar = false,
                    onNavigateToUploadInventory = onNavigateToUploadInventory,
                    onNavigateToAddListing = onNavigateToAddListing,
                    onNavigateToEditListing = onNavigateToEditListing,
                    onSearchTap = onNavigateToSearch,
                    onLogout = {}
                )
            } else if (pharmacyHomeViewModel != null) {
                PharmacyHomeScreen(viewModel = pharmacyHomeViewModel)
            }
        }
        HomeTab.CART -> pharmacyHomeViewModel?.let {
            PharmacyCartScreen(
                viewModel = it,
                onCheckoutAll = onCheckoutAll,
                onOpenSupplierOrder = onOpenSupplierOrder
            )
        }
        HomeTab.ORDERS -> if (sellerOrdersViewModel != null) {
            SellerOrdersScreen(
                viewModel = sellerOrdersViewModel,
                onOrderClick = onNavigateToSupplierOrderDetail,
                unreadOrderIds = unreadSupplierOrderIds
            )
        } else if (orderListViewModel != null) {
            OrderListScreen(
                viewModel = orderListViewModel,
                onOrderClick = onNavigateToOrderDetail,
                unreadOrderIds = unreadPharmacyOrderIds
            )
        }
        HomeTab.PROFILE -> ProfileScreen(
            viewModel = profileViewModel,
            user = currentUser,
            onOpenSettings = onNavigateToSettings,
            onLogout = onLogout
        )
    }
}

@Composable
private fun HomeTopBar(
    isSeller: Boolean,
    userName: String,
    unreadNotificationCount: Int,
    onNotificationsClick: () -> Unit,
    onCreateOrder: () -> Unit,
    onSearchClick: () -> Unit
) {
    Surface(shadowElevation = 4.dp, color = TopBarContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val strings = LocalStrings.current
            Text(
                text = strings.homeGreeting(userName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TopBarContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isSeller) {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = strings.pharmacySearchDrugName,
                            tint = TopBarContent
                        )
                    }
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
    cartBadgeCount: Int,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val tabs = buildList {
        add(HomeTab.HOME to GlassBarItem(Icons.Filled.Home, strings.navHome))
        if (!isSeller) add(HomeTab.CART to GlassBarItem(Icons.Filled.ShoppingCart, strings.navCart, cartBadgeCount))
        add(HomeTab.ORDERS to GlassBarItem(Icons.AutoMirrored.Filled.ReceiptLong, strings.navOrders, ordersBadgeCount))
        add(HomeTab.PROFILE to GlassBarItem(Icons.Filled.Person, strings.navProfile))
    }
    LiquidGlassBottomBar(
        items = tabs.map { it.second },
        selectedIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
        onItemSelected = { onTabSelected(tabs[it].first) },
        hazeState = hazeState,
        modifier = modifier
    )
}
