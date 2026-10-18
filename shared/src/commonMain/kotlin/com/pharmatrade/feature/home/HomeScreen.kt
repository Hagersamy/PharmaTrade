package com.pharmatrade.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.hints.DismissedHintsStore
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.core.ui.components.CoachMarkOverlay
import com.pharmatrade.core.ui.components.CoachMarkState
import com.pharmatrade.core.ui.components.CoachMarkStep
import com.pharmatrade.core.ui.components.coachMarkTarget
import com.pharmatrade.core.ui.components.rememberCoachMarkState
import com.pharmatrade.core.ui.components.GlassBarItem
import com.pharmatrade.core.ui.components.LiquidGlassBottomBar
import com.pharmatrade.core.ui.components.LocalBottomBarInset
import com.pharmatrade.core.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.delay
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

// MARKET is pharmacy-only. PROFILE has no bottom-bar item for pharmacies — it's opened from the
// avatar in the top bar.
enum class HomeTab { HOME, CART, ORDERS, MARKET, PROFILE }

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
    onLogout: () -> Unit,
    // The pharmacy-to-pharmacy marketplace tab's content; null for suppliers, who don't get the tab.
    marketplaceContent: (@Composable () -> Unit)? = null
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

    // First visit per role: a guided tour over the top-bar buttons and bottom tabs.
    val tourState = rememberCoachMarkState()
    val tourHint = if (isSeller) DismissedHintsStore.HOME_TOUR_SUPPLIER else DismissedHintsStore.HOME_TOUR_PHARMACY
    val tourSeen by remember(tourHint) { DismissedHintsStore.observe(tourHint) }.collectAsState(initial = true)
    // Short wait so every target has been laid out (and the screen has settled) before dimming it.
    var tourReady by remember { mutableStateOf(false) }
    LaunchedEffect(tourSeen) {
        tourReady = false
        if (!tourSeen) {
            delay(700)
            tourReady = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    tourState = tourState,
                    isSeller = isSeller,
                    userName = currentUser?.name.orEmpty(),
                    unreadNotificationCount = unreadNotificationCount,
                    onNotificationsClick = onNavigateToNotifications,
                    onCreateOrder = onNavigateToOrderMode,
                    onProfileClick = { onTabSelected(HomeTab.PROFILE) },
                    onSearchClick = { onPharmacySearchOpenChange(true) }
                )
            },
            // Matches the tab screens so the top bar's rounded corners sit on the same backdrop.
            containerColor = BackgroundGray,
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
                            onLogout = onLogout,
                            marketplaceContent = marketplaceContent
                        )
                    }
                }
                HomeBottomBar(
                    tourState = tourState,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    isSeller = isSeller,
                    hasMarketplace = marketplaceContent != null,
                    ordersBadgeCount = pharmacyHomeState?.activeOrdersTotal ?: 0,
                    cartBadgeCount = pharmacyHomeState?.totalCartItems ?: 0,
                    hazeState = hazeState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { bottomBarHeight = with(density) { it.height.toDp() } }
                )
            }
        }

        if (tourReady && currentUser != null) {
            val strings = LocalStrings.current
            CoachMarkOverlay(
                state = tourState,
                steps = homeTourSteps(isSeller, hasMarketplace = marketplaceContent != null),
                skipLabel = strings.introSkip,
                nextLabel = strings.introNext,
                doneLabel = strings.tourDone,
                onFinish = { DismissedHintsStore.dismiss(tourHint) }
            )
        }
    }
}

private object TourTarget {
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val CREATE_ORDER = "create_order"
    const val NOTIFICATIONS = "notifications"
    fun tab(tab: HomeTab) = "tab_${tab.name}"
}

@Composable
private fun homeTourSteps(isSeller: Boolean, hasMarketplace: Boolean): List<CoachMarkStep> {
    val s = LocalStrings.current
    return if (isSeller) listOf(
        CoachMarkStep(TourTarget.NOTIFICATIONS, s.tourNotificationsTitle, s.tourNotificationsBody),
        CoachMarkStep(TourTarget.tab(HomeTab.HOME), s.tourHomeTitle, s.tourHomeBodySupplier),
        CoachMarkStep(TourTarget.tab(HomeTab.ORDERS), s.tourOrdersTitle, s.tourOrdersBodySupplier),
        CoachMarkStep(TourTarget.tab(HomeTab.PROFILE), s.tourProfileTitle, s.tourProfileBodySupplier)
    ) else listOfNotNull(
        CoachMarkStep(TourTarget.PROFILE, s.tourProfileTitle, s.tourProfileBodyPharmacy),
        CoachMarkStep(TourTarget.SEARCH, s.tourSearchTitle, s.tourSearchBody),
        CoachMarkStep(TourTarget.CREATE_ORDER, s.tourCreateOrderTitle, s.tourCreateOrderBody),
        CoachMarkStep(TourTarget.NOTIFICATIONS, s.tourNotificationsTitle, s.tourNotificationsBody),
        CoachMarkStep(TourTarget.tab(HomeTab.HOME), s.tourHomeTitle, s.tourHomeBodyPharmacy),
        CoachMarkStep(TourTarget.tab(HomeTab.CART), s.tourCartTitle, s.tourCartBody),
        CoachMarkStep(TourTarget.tab(HomeTab.ORDERS), s.tourOrdersTitle, s.tourOrdersBodyPharmacy),
        if (hasMarketplace) CoachMarkStep(TourTarget.tab(HomeTab.MARKET), s.tourMarketTitle, s.tourMarketBody) else null
    )
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
    onLogout: () -> Unit,
    marketplaceContent: (@Composable () -> Unit)?
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
        HomeTab.MARKET -> marketplaceContent?.invoke()
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
    tourState: CoachMarkState,
    isSeller: Boolean,
    userName: String,
    unreadNotificationCount: Int,
    onNotificationsClick: () -> Unit,
    onCreateOrder: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val strings = LocalStrings.current
    val name = userName.trim()
    Surface(
        color = TopBarContainer,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = name,
                onClick = onProfileClick,
                modifier = Modifier.coachMarkTarget(tourState, TourTarget.PROFILE)
            )
            Spacer(Modifier.width(12.dp))
            // Greeting on a quiet first line, the name as the strong second line: the eye lands
            // on the name, and a long name gets the whole row width instead of sharing it.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.homeGreeting(""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TopBarContent.copy(alpha = 0.75f),
                    maxLines = 1
                )
                if (name.isNotEmpty()) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TopBarContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isSeller) {
                    TopBarAction(
                        icon = Icons.Filled.Search,
                        contentDescription = strings.pharmacySearchDrugName,
                        onClick = onSearchClick,
                        modifier = Modifier.coachMarkTarget(tourState, TourTarget.SEARCH)
                    )
                    TopBarAction(
                        icon = Icons.Filled.AddShoppingCart,
                        contentDescription = strings.homeCreateOrderContentDescription,
                        onClick = onCreateOrder,
                        modifier = Modifier.coachMarkTarget(tourState, TourTarget.CREATE_ORDER)
                    )
                }
                TopBarAction(
                    icon = if (unreadNotificationCount > 0) Icons.Filled.NotificationsActive else Icons.Filled.Notifications,
                    contentDescription = strings.notificationsTitle,
                    badgeCount = unreadNotificationCount,
                    onClick = onNotificationsClick,
                    modifier = Modifier.coachMarkTarget(tourState, TourTarget.NOTIFICATIONS)
                )
            }
        }
    }
}

@Composable
private fun UserAvatar(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val initials = name.split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(TopBarContent.copy(alpha = 0.16f))
            .border(1.5.dp, TopBarContent.copy(alpha = 0.35f), CircleShape)
            .clickable(onClickLabel = strings.navProfile, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotEmpty()) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TopBarContent
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = TopBarContent
            )
        }
    }
}

@Composable
private fun TopBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    // The badge sits outside the button so the circular clip doesn't cut it off.
    BadgedBox(
        modifier = modifier,
        badge = {
            if (badgeCount > 0) {
                Badge(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White,
                    modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                ) {
                    Text(if (badgeCount > 99) "99+" else "$badgeCount")
                }
            }
        }
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(42.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = TopBarContent.copy(alpha = 0.14f),
                contentColor = TopBarContent
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    tourState: CoachMarkState,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    isSeller: Boolean,
    hasMarketplace: Boolean,
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
        // Pharmacies reach Profile through the top bar's avatar; its slot goes to the marketplace.
        if (hasMarketplace) {
            add(HomeTab.MARKET to GlassBarItem(Icons.Filled.Handshake, strings.navMarketplace))
        } else {
            add(HomeTab.PROFILE to GlassBarItem(Icons.Filled.Person, strings.navProfile))
        }
    }
    LiquidGlassBottomBar(
        items = tabs.map { it.second },
        selectedIndex = tabs.indexOfFirst { it.first == selectedTab },
        onItemSelected = { onTabSelected(tabs[it].first) },
        hazeState = hazeState,
        modifier = modifier,
        itemModifier = { Modifier.coachMarkTarget(tourState, TourTarget.tab(tabs[it].first)) }
    )
}
