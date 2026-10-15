package com.pharmatrade.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.di.AppContainer
import com.pharmatrade.feature.admin.presentation.AdminDashboardScreen
import com.pharmatrade.feature.admin.presentation.AdminDashboardViewModel
import com.pharmatrade.feature.admin.presentation.AnalyticsViewModel
import com.pharmatrade.feature.auth.presentation.login.LoginScreen
import com.pharmatrade.feature.auth.presentation.welcome.WelcomeScreen
import com.pharmatrade.feature.auth.presentation.login.LoginViewModel
import com.pharmatrade.feature.auth.presentation.pending.PendingApprovalScreen
import com.pharmatrade.feature.auth.presentation.pending.RegistrationOutcome
import com.pharmatrade.feature.auth.presentation.register.RegisterScreen
import com.pharmatrade.feature.auth.presentation.register.RegisterViewModel
import com.pharmatrade.feature.cart.presentation.CartScreen
import com.pharmatrade.feature.cart.presentation.CartViewModel
import com.pharmatrade.feature.catalog.presentation.drugs.SellerDrugsScreen
import com.pharmatrade.feature.catalog.presentation.drugs.SellerDrugsViewModel
import com.pharmatrade.feature.home.HomeScreen
import com.pharmatrade.feature.home.HomeTab
import com.pharmatrade.feature.profile.presentation.ProfileScreen
import com.pharmatrade.feature.profile.presentation.ProfileViewModel
import com.pharmatrade.feature.profile.presentation.SettingsScreen
import com.pharmatrade.feature.profile.presentation.SettingsViewModel
import com.pharmatrade.BuildConfig
import com.pharmatrade.R
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.pharmatrade.feature.marketplace.presentation.collection.MarketCollectionScreen
import com.pharmatrade.feature.marketplace.presentation.collection.MarketCollectionViewModel
import com.pharmatrade.feature.marketplace.presentation.hub.MarketBrowseViewModel
import com.pharmatrade.feature.marketplace.presentation.hub.MarketInventoryViewModel
import com.pharmatrade.feature.marketplace.presentation.hub.MarketOrdersViewModel
import com.pharmatrade.feature.marketplace.presentation.hub.MarketTab
import com.pharmatrade.feature.marketplace.presentation.hub.MarketplaceScreen
import com.pharmatrade.feature.marketplace.presentation.hub.MarketplaceTabViewModels
import com.pharmatrade.feature.marketplace.presentation.hub.MyCollectionsViewModel
import com.pharmatrade.feature.marketplace.presentation.order.MarketOrderScreen
import com.pharmatrade.feature.marketplace.presentation.order.MarketOrderViewModel
import com.pharmatrade.feature.notification.presentation.NotificationViewModel
import com.pharmatrade.feature.notification.presentation.NotificationsScreen
import com.pharmatrade.push.NotificationDeepLink
import com.pharmatrade.feature.pharmacyorder.domain.model.DraftOrderItem
import com.pharmatrade.feature.pharmacyorder.presentation.allocation.AllocationScreen
import com.pharmatrade.feature.pharmacyorder.presentation.allocation.AllocationViewModel
import com.pharmatrade.feature.pharmacyorder.presentation.checkout.CheckoutScreen
import com.pharmatrade.feature.pharmacyorder.presentation.checkout.CheckoutViewModel
import com.pharmatrade.feature.pharmacyorder.presentation.home.PharmacyHomeViewModel
import com.pharmatrade.feature.pharmacyorder.presentation.orderdetail.OrderDetailScreen
import com.pharmatrade.feature.pharmacyorder.presentation.orderdetail.OrderDetailViewModel
import com.pharmatrade.feature.pharmacyorder.presentation.orderitems.OrderItemsScreen
import com.pharmatrade.feature.pharmacyorder.presentation.orderitems.OrderItemsViewModel
import com.pharmatrade.feature.pharmacyorder.presentation.orderlist.OrderListViewModel
import com.pharmatrade.feature.pharmacyorder.presentation.ordermode.OrderModeScreen
import com.pharmatrade.feature.pharmacyorder.presentation.ordermode.OrderModeViewModel
import com.pharmatrade.feature.search.BuyerSearchScreen
import com.pharmatrade.feature.search.BuyerSearchViewModel
import com.pharmatrade.feature.search.SellerSearchScreen
import com.pharmatrade.feature.search.SellerSearchViewModel
import com.pharmatrade.feature.seller.presentation.dashboard.SellerDashboardViewModel
import com.pharmatrade.feature.seller.presentation.drug_form.DrugFormScreen
import com.pharmatrade.feature.seller.presentation.drug_form.DrugFormViewModel
import com.pharmatrade.feature.seller.presentation.inventory.InventoryUploadScreen
import com.pharmatrade.feature.seller.presentation.inventory.InventoryUploadViewModel
import com.pharmatrade.feature.supplierorder.presentation.orderdetail.SellerOrderDetailScreen
import com.pharmatrade.feature.supplierorder.presentation.orderdetail.SellerOrderDetailViewModel
import com.pharmatrade.feature.supplierorder.presentation.orderlist.SellerOrdersViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(container: AppContainer, pendingDeepLink: MutableState<NotificationDeepLink?>) {
    val startKey = remember {
        when {
            !SessionManager.isLoggedIn -> NavKeys.Welcome
            SessionManager.user?.isPending == true -> NavKeys.PendingApproval
            SessionManager.user?.isDeclined == true -> NavKeys.RegistrationDeclined
            SessionManager.user?.userType == UserType.ADMIN -> NavKeys.AdminDashboard
            else -> NavKeys.Home
        }
    }
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(startKey)
    val navigator = remember { Navigator(backStack) }
    val coroutineScope = rememberCoroutineScope()
    val tabHistory = rememberSaveable(
        saver = listSaver(
            save = { it.map(HomeTab::name) },
            restore = { it.map(HomeTab::valueOf).toMutableStateList() })
    ) { mutableStateListOf(HomeTab.HOME) }
    val homeSelectedTab = tabHistory.last()
    var isPharmacySearchOpen by rememberSaveable { mutableStateOf(false) }
    var marketTab by rememberSaveable { mutableStateOf(MarketTab.BROWSE) }
    fun selectHomeTab(tab: HomeTab) {
        if (tab == HomeTab.HOME) {
            // Also the reset every logout path runs, so the next login starts on plain Home.
            isPharmacySearchOpen = false
            tabHistory.clear()
            tabHistory.add(HomeTab.HOME)
        } else if (tab != tabHistory.last()) {
            tabHistory.add(tab)
        }
    }
    // The pharmacy search screen is shown inside Home (it shares Home's ViewModel), so back has to
    // close it here, before the tab history gets a turn.
    BackHandler(enabled = backStack.lastOrNull() == NavKeys.Home && isPharmacySearchOpen) {
        isPharmacySearchOpen = false
    }
    BackHandler(enabled = backStack.lastOrNull() == NavKeys.Home && !isPharmacySearchOpen && tabHistory.size > 1) {
        tabHistory.removeAt(tabHistory.lastIndex)
    }

    val cartViewModel: CartViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CartViewModel(
                    getCartUseCase = container.getCartUseCase,
                    addToCartUseCase = container.addToCartUseCase,
                    updateItemUseCase = container.updateCartItemUseCase,
                    removeItemUseCase = container.removeFromCartUseCase,
                    clearCartUseCase = container.clearCartUseCase,
                    validateCartUseCase = container.validateCartUseCase,
                    placeOrderUseCase = container.placeOrderUseCase
                )
            }
        }
    )

    val cartState by cartViewModel.uiState.collectAsState()

    // Hoisted here (not inside entry<NavKeys.Notifications>) so the same instance backs both the
    // Home bell icon's unread badge and the notifications list screen — marking everything read
    // there must be reflected back on the badge without waiting for Home to recreate its ViewModels.
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                NotificationViewModel(
                    getNotificationsUseCase = container.getNotificationsUseCase,
                    getUnreadCountUseCase = container.getUnreadNotificationCountUseCase,
                    markAsReadUseCase = container.markNotificationAsReadUseCase,
                    markAllAsReadUseCase = container.markAllNotificationsAsReadUseCase,
                    deleteNotificationUseCase = container.deleteNotificationUseCase,
                    clearAllNotificationsUseCase = container.clearAllNotificationsUseCase
                )
            }
        }
    )
    val notificationState by notificationViewModel.uiState.collectAsState()
    val deepLink by pendingDeepLink
    LaunchedEffect(deepLink) {
        val link = deepLink ?: return@LaunchedEffect
        if (SessionManager.isLoggedIn) {
            if (isMarketplaceNotification(link.type)) {
                selectHomeTab(HomeTab.MARKET)
                navigator.popBackTo(NavKeys.Home)
            } else {
                resolveNotificationDestination(link.type, link.notifiableId)?.let {
                    navigator.navigate(
                        it
                    )
                }
            }
        }
        pendingDeepLink.value = null
    }
    val sessionUser by SessionManager.currentUser.collectAsState()
    LaunchedEffect(sessionUser) {
        // Session lost (logout, expired token) while inside the app → back to Welcome. Already on
        // one of the logged-out screens means there's nothing to redirect.
        val onAuthScreen = backStack.lastOrNull().let {
            it == NavKeys.Welcome || it == NavKeys.Login || it == NavKeys.Register
        }
        if (sessionUser == null && !onAuthScreen) {
            selectHomeTab(HomeTab.HOME)
            navigator.navigate(NavKeys.Welcome, popUpTo = Navigator.PopUpTo(target = null))
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {

            entry<NavKeys.Welcome> {
                WelcomeScreen(
                    appLogo = rememberAppLogo(),
                    onLoginClick = { navigator.navigate(NavKeys.Login) },
                    onRegisterClick = { navigator.navigate(NavKeys.Register) }
                )
            }

            entry<NavKeys.Login> {
                val vm: LoginViewModel = viewModel(
                    factory = viewModelFactory { initializer { LoginViewModel(container.loginUseCase) } }
                )
                LoginScreen(
                    viewModel = vm,
                    appLogo = rememberAppLogo(),
                    onNavigateToRegister = { navigator.navigate(NavKeys.Register) },
                    onNavigateToSellerDashboard = {
                        navigator.navigate(
                            NavKeys.Home,
                            popUpTo = Navigator.PopUpTo(target = null)
                        )
                    },
                    onNavigateToBuyerCatalog = {
                        navigator.navigate(
                            NavKeys.Home,
                            popUpTo = Navigator.PopUpTo(target = null)
                        )
                    },
                    onNavigateToAdminDashboard = {
                        navigator.navigate(
                            NavKeys.AdminDashboard,
                            popUpTo = Navigator.PopUpTo(target = null)
                        )
                    },
                    onNavigateToPendingApproval = {
                        navigator.navigate(
                            NavKeys.PendingApproval,
                            popUpTo = Navigator.PopUpTo(target = null)
                        )
                    },
                    onNavigateToRegistrationDeclined = {
                        navigator.navigate(
                            NavKeys.RegistrationDeclined,
                            popUpTo = Navigator.PopUpTo(target = null)
                        )
                    }
                )
            }

            entry<NavKeys.Register> {
                val vm: RegisterViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            RegisterViewModel(
                                registerUseCase = container.registerUseCase,
                                zoneRepository = container.zoneRepository
                            )
                        }
                    }
                )
                RegisterScreen(
                    viewModel = vm,
                    onNavigateToPendingApproval = {
                        navigator.navigate(
                            NavKeys.PendingApproval,
                            popUpTo = Navigator.PopUpTo(target = null)
                        )
                    }
                )
            }

            entry<NavKeys.PendingApproval> {
                val userName = SessionManager.user?.name ?: ""
                PendingApprovalScreen(
                    userName = userName,
                    outcome = RegistrationOutcome.PENDING,
                    onBackToLogin = {
                        coroutineScope.launch { container.logoutUseCase() }
                        // Reset the Home tab-visit history so the next login always lands back on
                        // the Home tab, instead of resuming whatever tab (e.g. Profile) was open
                        // when this session logged out.
                        selectHomeTab(HomeTab.HOME)
                        navigator.navigate(NavKeys.Welcome, popUpTo = Navigator.PopUpTo(target = null))
                    }
                )
            }

            // Reachable either at cold-start (a previously-restored session whose account was
            // declined since the last login) or by tapping a "registration_declined" notification.
            entry<NavKeys.RegistrationDeclined> {
                val userName = SessionManager.user?.name ?: ""
                PendingApprovalScreen(
                    userName = userName,
                    outcome = RegistrationOutcome.DECLINED,
                    onBackToLogin = {
                        coroutineScope.launch { container.logoutUseCase() }
                        selectHomeTab(HomeTab.HOME)
                        navigator.navigate(NavKeys.Welcome, popUpTo = Navigator.PopUpTo(target = null))
                    }
                )
            }

            entry<NavKeys.AdminDashboard> {
                val vm: AdminDashboardViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            AdminDashboardViewModel(
                                getRegistrationRequestsUseCase = container.getRegistrationRequestsUseCase,
                                approveRequestUseCase = container.approveRequestUseCase,
                                declineRequestUseCase = container.declineRequestUseCase,
                                getRegistrationStatsUseCase = container.getRegistrationStatsUseCase
                            )
                        }
                    }
                )
                val analyticsVm: AnalyticsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            AnalyticsViewModel(
                                getStatsUseCase = container.getRegistrationStatsUseCase,
                                getRequestsUseCase = container.getRegistrationRequestsUseCase,
                                approveRequestUseCase = container.approveRequestUseCase,
                                declineRequestUseCase = container.declineRequestUseCase,
                                getZones = container.zoneRepository::getZones
                            )
                        }
                    }
                )
                val adminProfileVm: ProfileViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            ProfileViewModel(
                                getBranchUseCase = container.getBranchUseCase,
                                getProfileUseCase = container.getProfileUseCase,
                                updateProfileUseCase = container.updateProfileUseCase,
                                changePasswordUseCase = container.changePasswordUseCase,
                                updateSupplierProfileUseCase = container.updateSupplierProfileUseCase,
                                updateBranchProfileUseCase = container.updateBranchProfileUseCase,
                                requestZoneUpdateUseCase = container.requestZoneUpdateUseCase,
                                zoneRepository = container.zoneRepository
                            )
                        }
                    }
                )
                RefreshOnResume { notificationViewModel.refreshUnreadCount() }
                AdminDashboardScreen(
                    viewModel = vm,
                    analyticsViewModel = analyticsVm,
                    unreadNotificationCount = notificationState.unreadCount,
                    onNavigateToNotifications = { navigator.navigate(NavKeys.Notifications) },
                    profileContent = {
                        val adminUser by SessionManager.currentUser.collectAsState()
                        ProfileScreen(
                            viewModel = adminProfileVm,
                            user = adminUser,
                            onOpenSettings = { navigator.navigate(NavKeys.Settings) },
                            onLogout = {
                                coroutineScope.launch { container.logoutUseCase() }
                                selectHomeTab(HomeTab.HOME)
                                navigator.navigate(NavKeys.Welcome, popUpTo = Navigator.PopUpTo(target = null))
                            }
                        )
                    }
                )
            }

            entry<NavKeys.Home> {
                // Only build the logged-in role's ViewModels — each one loads its data in init{},
                // so creating both sets made a supplier call pharmacy/* (and a pharmacy call
                // supplier/*), all rejected with 403. Read once for this entry: logging out pops
                // Home, and the next login gets a fresh entry with the new role.
                val userType = remember { SessionManager.currentUser.value?.userType }
                val isSeller = userType == UserType.SELLER
                val isPharmacy = userType != null && !isSeller

                val sellerVm: SellerDashboardViewModel? = if (isSeller) viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SellerDashboardViewModel(
                                getProfileUseCase = container.getSellerProfileUseCase,
                                getInventoryUseCase = container.getInventoryUseCase,
                                updateMinOrderUseCase = container.updateMinOrderUseCase,
                                updateInventoryItemUseCase = container.updateInventoryItemUseCase
                            )
                        }
                    }
                ) else null
                val pharmacyHomeVm: PharmacyHomeViewModel? = if (isPharmacy) viewModel(
                    factory = viewModelFactory {
                        initializer {
                            PharmacyHomeViewModel(
                                getPharmacyOrdersUseCase = container.getPharmacyOrdersUseCase,
                                getAllSuppliersDrugsUseCase = container.getAllSuppliersDrugsUseCase,
                                createOrderUseCase = container.createOrderUseCase,
                                addOrderItemUseCase = container.addOrderItemUseCase,
                                removeOrderItemUseCase = container.removeOrderItemUseCase,
                                cancelOrderUseCase = container.cancelOrderUseCase,
                                cartStore = container.pharmacyCartStore
                            )
                        }
                    }
                ) else null
                val orderListVm: OrderListViewModel? = if (isPharmacy) viewModel(
                    factory = viewModelFactory {
                        initializer { OrderListViewModel(getPharmacyOrdersUseCase = container.getPharmacyOrdersUseCase) }
                    }
                ) else null
                val sellerOrdersVm: SellerOrdersViewModel? = if (isSeller) viewModel(
                    factory = viewModelFactory {
                        initializer { SellerOrdersViewModel(getSupplierOrdersUseCase = container.getSupplierOrdersUseCase) }
                    }
                ) else null
                val marketplaceVms: MarketplaceTabViewModels? = if (isPharmacy) MarketplaceTabViewModels(
                    browse = viewModel(
                        factory = viewModelFactory {
                            initializer { MarketBrowseViewModel(browseCollectionsUseCase = container.browseCollectionsUseCase) }
                        }
                    ),
                    purchases = viewModel(
                        key = "market_purchases",
                        factory = viewModelFactory {
                            initializer { MarketOrdersViewModel(loadOrders = { container.getMarketPurchasesUseCase() }) }
                        }
                    ),
                    collections = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                MyCollectionsViewModel(
                                    getMyCollectionsUseCase = container.getMyCollectionsUseCase,
                                    createCollectionUseCase = container.createCollectionUseCase
                                )
                            }
                        }
                    ),
                    inventory = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                MarketInventoryViewModel(
                                    getMarketInventoryUseCase = container.getMarketInventoryUseCase,
                                    addMarketInventoryItemUseCase = container.addMarketInventoryItemUseCase,
                                    updateMarketInventoryItemUseCase = container.updateMarketInventoryItemUseCase,
                                    importMarketInventoryUseCase = container.importMarketInventoryUseCase,
                                    getDrugsUseCase = container.getDrugsUseCase
                                )
                            }
                        }
                    ),
                    sales = viewModel(
                        key = "market_sales",
                        factory = viewModelFactory {
                            initializer { MarketOrdersViewModel(loadOrders = { container.getMarketSalesUseCase() }) }
                        }
                    )
                ) else null
                val profileVm: ProfileViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            ProfileViewModel(
                                getBranchUseCase = container.getBranchUseCase,
                                getProfileUseCase = container.getProfileUseCase,
                                updateProfileUseCase = container.updateProfileUseCase,
                                changePasswordUseCase = container.changePasswordUseCase,
                                updateSupplierProfileUseCase = container.updateSupplierProfileUseCase,
                                updateBranchProfileUseCase = container.updateBranchProfileUseCase,
                                requestZoneUpdateUseCase = container.requestZoneUpdateUseCase,
                                zoneRepository = container.zoneRepository
                            )
                        }
                    }
                )
                RefreshOnResume {
                    sellerVm?.loadData()
                    pharmacyHomeVm?.loadActiveOrdersCount()
                    notificationViewModel.refreshUnreadCount()
                    if (homeSelectedTab == HomeTab.ORDERS) {
                        orderListVm?.loadOrders()
                        sellerOrdersVm?.loadOrders()
                        notificationViewModel.refreshList()
                    }
                    // Back from a collection/order — its actions change these lists.
                    if (homeSelectedTab == HomeTab.MARKET) marketplaceVms?.refresh(marketTab)
                }
                LaunchedEffect(homeSelectedTab) {
                    if (homeSelectedTab == HomeTab.ORDERS) {
                        orderListVm?.loadOrders()
                        sellerOrdersVm?.loadOrders()
                        notificationViewModel.refreshList()
                    }
                    if (homeSelectedTab == HomeTab.MARKET) marketplaceVms?.refresh(marketTab)
                }
                HomeScreen(
                    sellerDashboardViewModel = sellerVm,
                    pharmacyHomeViewModel = pharmacyHomeVm,
                    orderListViewModel = orderListVm,
                    sellerOrdersViewModel = sellerOrdersVm,
                    profileViewModel = profileVm,
                    selectedTab = homeSelectedTab,
                    onTabSelected = ::selectHomeTab,
                    isPharmacySearchOpen = isPharmacySearchOpen,
                    onPharmacySearchOpenChange = { isPharmacySearchOpen = it },
                    unreadNotificationCount = notificationState.unreadCount,
                    unreadPharmacyOrderIds = notificationState.unreadPharmacyOrderIds,
                    unreadSupplierOrderIds = notificationState.unreadSupplierOrderIds,
                    onNavigateToNotifications = { navigator.navigate(NavKeys.Notifications) },
                    onNavigateToSettings = { navigator.navigate(NavKeys.Settings) },
                    onNavigateToSearch = {
                        val key =
                            if (SessionManager.currentUser.value?.userType == UserType.SELLER) {
                                NavKeys.SellerSearch
                            } else {
                                NavKeys.BuyerSearch
                            }
                        navigator.navigate(key)
                    },
                    onNavigateToUploadInventory = { navigator.navigate(NavKeys.InventoryUpload) },
                    onNavigateToAddListing = { navigator.navigate(NavKeys.AddListing) },
                    onNavigateToEditListing = { listingId ->
                        navigator.navigate(NavKeys.EditListing(listingId))
                    },
                    onNavigateToOrderMode = { navigator.navigate(NavKeys.OrderMode) },
                    onNavigateToOrderDetail = { orderId ->
                        navigator.navigate(NavKeys.OrderDetail(orderId))
                    },
                    onNavigateToSupplierOrderDetail = { orderId ->
                        navigator.navigate(NavKeys.SupplierOrderDetail(orderId))
                    },
                    onCheckoutAll = { orders ->
                        navigator.navigate(
                            NavKeys.Checkout(orders.map { (orderId, supplierId) ->
                                OrderSupplierPair(
                                    orderId,
                                    supplierId
                                )
                            })
                        )
                    },
                    onOpenSupplierOrder = { orderId, supplierId, lines ->
                        navigator.navigate(
                            NavKeys.OrderItems(
                                orderId = orderId,
                                supplierId = supplierId,
                                initialLines = lines.map {
                                    DraftOrderLine(
                                        it.id,
                                        it.drugId,
                                        it.drugName,
                                        it.quantity
                                    )
                                }
                            )
                        )
                    },
                    onLogout = {
                        coroutineScope.launch { container.logoutUseCase() }
                        selectHomeTab(HomeTab.HOME)
                        navigator.navigate(NavKeys.Welcome, popUpTo = Navigator.PopUpTo(target = null))
                    },
                    marketplaceContent = marketplaceVms?.let { vms ->
                        {
                            MarketplaceScreen(
                                viewModels = vms,
                                selectedTab = marketTab,
                                onTabSelected = { tab ->
                                    marketTab = tab
                                    vms.refresh(tab)
                                },
                                onOpenCollection = { id, mine -> navigator.navigate(NavKeys.MarketCollection(id, mine)) },
                                onOpenOrder = { id, asSeller -> navigator.navigate(NavKeys.MarketOrder(id, asSeller)) }
                            )
                        }
                    }
                )
            }

            entry<NavKeys.Checkout> { key ->
                val vm: CheckoutViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            CheckoutViewModel(
                                orderSupplierPairs = key.orders.map { it.orderId to it.supplierId },
                                allocateOrderUseCase = container.allocateOrderUseCase,
                                cancelOrderUseCase = container.cancelOrderUseCase
                            )
                        }
                    }
                )
                CheckoutScreen(
                    viewModel = vm,
                    onNavigateBack = { navigator.goBack() },
                    onDone = {
                        navigator.popBackTo(NavKeys.Home)
                    }
                )
            }

            entry<NavKeys.Notifications> {
                NotificationsScreen(
                    viewModel = notificationViewModel,
                    onNavigateBack = { navigator.goBack() },
                    onNotificationClick = { notification ->
                        if (isMarketplaceNotification(notification.type)) {
                            selectHomeTab(HomeTab.MARKET)
                            navigator.popBackTo(NavKeys.Home)
                        } else {
                            resolveNotificationDestination(notification.type, notification.notifiableId)
                                ?.let { navigator.navigate(it) }
                        }
                    }
                )
            }

            entry<NavKeys.Settings> {
                val vm: SettingsViewModel = viewModel { SettingsViewModel() }
                SettingsScreen(
                    viewModel = vm,
                    appVersion = BuildConfig.VERSION_NAME,
                    onNavigateBack = { navigator.goBack() }
                )
            }

            entry<NavKeys.BuyerSearch> {
                val vm: BuyerSearchViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            BuyerSearchViewModel(
                                getAllSellersUseCase = container.getAllSellersUseCase,
                                searchListingsUseCase = container.searchListingsUseCase
                            )
                        }
                    }
                )
                BuyerSearchScreen(
                    viewModel = vm,
                    onNavigateBack = { navigator.goBack() },
                    onNavigateToSellerDrugs = { sellerId ->
                        navigator.navigate(NavKeys.SellerDrugs(sellerId))
                    }
                )
            }

            entry<NavKeys.SellerSearch> {
                val vm: SellerSearchViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SellerSearchViewModel(
                                getListingsUseCase = container.getSellerListingsUseCase,
                                getDrugsUseCase = container.getDrugsUseCase
                            )
                        }
                    }
                )
                SellerSearchScreen(
                    viewModel = vm,
                    onNavigateBack = { navigator.goBack() },
                    onNavigateToEdit = { listingId ->
                        navigator.navigate(NavKeys.EditListing(listingId))
                    },
                    onAddListing = { navigator.navigate(NavKeys.AddListing) }
                )
            }

            entry<NavKeys.InventoryUpload> {
                val vm: InventoryUploadViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            InventoryUploadViewModel(
                                uploadInventoryUseCase = container.uploadInventoryUseCase,
                                getInventoryUseCase = container.getInventoryUseCase
                            )
                        }
                    }
                )
                InventoryUploadScreen(
                    viewModel = vm,
                    onNavigateBack = { navigator.goBack() }
                )
            }

            entry<NavKeys.AddListing> {
                val vm: DrugFormViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            DrugFormViewModel(
                                createInventoryItemUseCase = container.createInventoryItemUseCase,
                                updateListingUseCase = container.updateListingUseCase,
                                getDrugsUseCase = container.getDrugsUseCase,
                                createSupplierDrugUseCase = container.createSupplierDrugUseCase
                            )
                        }
                    }
                )
                DrugFormScreen(
                    viewModel = vm,
                    editingListingId = null,
                    onNavigateBack = { navigator.goBack() },
                    onSaved = { navigator.goBack() }
                )
            }

            entry<NavKeys.EditListing> { key ->
                val vm: DrugFormViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            DrugFormViewModel(
                                createInventoryItemUseCase = container.createInventoryItemUseCase,
                                updateListingUseCase = container.updateListingUseCase,
                                getDrugsUseCase = container.getDrugsUseCase,
                                createSupplierDrugUseCase = container.createSupplierDrugUseCase
                            )
                        }
                    }
                )
                DrugFormScreen(
                    viewModel = vm,
                    editingListingId = key.listingId,
                    onNavigateBack = { navigator.goBack() },
                    onSaved = { navigator.goBack() }
                )
            }

            entry<NavKeys.SellerDrugs> { key ->
                val vm: SellerDrugsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SellerDrugsViewModel(
                                getListingsUseCase = container.getCatalogSellerListingsUseCase,
                                getAllSellersUseCase = container.getAllSellersUseCase
                            )
                        }
                    }
                )
                SellerDrugsScreen(
                    viewModel = vm,
                    sellerId = key.sellerId,
                    onNavigateBack = { navigator.goBack() },
                    onNavigateToCart = { navigator.navigate(NavKeys.Cart) },
                    onAddToCart = { listing -> cartViewModel.addToCart(listing) },
                    cartItemCount = cartState.cart.getTotalItems()
                )
            }

            entry<NavKeys.Cart> {
                CartScreen(
                    viewModel = cartViewModel,
                    onNavigateBack = { navigator.goBack() },
                    onOrderSuccess = { orderId ->
                        navigator.navigate(
                            NavKeys.OrderSuccess(orderId),
                            popUpTo = Navigator.PopUpTo(NavKeys.Home)
                        )
                    }
                )
            }

            entry<NavKeys.OrderSuccess> { key ->
                OrderSuccessScreen(
                    orderId = key.orderId,
                    onNavigateHome = {
                        navigator.popBackTo(NavKeys.Home)
                    }
                )
            }

            entry<NavKeys.OrderMode> {
                val vm: OrderModeViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            OrderModeViewModel(
                                getSuppliersUseCase = container.getSuppliersUseCase,
                                createOrderUseCase = container.createOrderUseCase
                            )
                        }
                    }
                )
                OrderModeScreen(
                    viewModel = vm,
                    onNavigateBack = { navigator.goBack() },
                    onOrderCreated = { orderId, supplierId ->
                        navigator.navigate(
                            NavKeys.OrderItems(orderId, supplierId),
                            popUpTo = Navigator.PopUpTo(NavKeys.OrderMode, inclusive = true)
                        )
                    }
                )
            }

            entry<NavKeys.OrderItems> { key ->
                val vm: OrderItemsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            OrderItemsViewModel(
                                orderId = key.orderId,
                                supplierId = key.supplierId,
                                getDrugsUseCase = container.getDrugsUseCase,
                                getSupplierInventoryUseCase = container.getSupplierInventoryUseCase,
                                addOrderItemUseCase = container.addOrderItemUseCase,
                                removeOrderItemUseCase = container.removeOrderItemUseCase,
                                uploadOrderItemsUseCase = container.uploadOrderItemsUseCase,
                                getOrderDetailUseCase = container.getOrderDetailUseCase,
                                initialItems = key.initialLines.map {
                                    DraftOrderItem(
                                        it.id,
                                        it.drugId,
                                        it.drugName,
                                        it.quantity
                                    )
                                }
                            )
                        }
                    }
                )
                // +/- taps are sent after a short debounce — send any still waiting before leaving,
                // or they'd be lost with the ViewModel (and missing from the order handed to the cart).
                BackHandler { vm.syncPendingThen { navigator.goBack() } }
                OrderItemsScreen(
                    viewModel = vm,
                    onNavigateBack = { vm.syncPendingThen { navigator.goBack() } },
                    onReviewAndAllocate = {
                        if (key.supplierId.isNullOrBlank()) {
                            navigator.navigate(NavKeys.Allocation(key.orderId, key.supplierId))
                        } else {
                            // A supplier's order goes to the Cart tab (where it can be checked out
                            // with the rest), not straight to allocation.
                            vm.publishToCart()
                            selectHomeTab(HomeTab.CART)
                            navigator.popBackTo(NavKeys.Home)
                        }
                    }
                )
            }

            entry<NavKeys.Allocation> { key ->
                val vm: AllocationViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            AllocationViewModel(
                                orderId = key.orderId,
                                supplierId = key.supplierId,
                                allocateOrderUseCase = container.allocateOrderUseCase,
                                cancelOrderUseCase = container.cancelOrderUseCase
                            )
                        }
                    }
                )
                AllocationScreen(
                    viewModel = vm,
                    onNavigateBack = { navigator.goBack() },
                    onConfirmed = { confirmedOrderId ->
                        navigator.navigate(
                            NavKeys.OrderDetail(confirmedOrderId),
                            popUpTo = Navigator.PopUpTo(NavKeys.Home)
                        )
                    },
                    onCancelled = {
                        navigator.popBackTo(NavKeys.Home)
                    }
                )
            }

            entry<NavKeys.OrderDetail> { key ->
                val vm: OrderDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            OrderDetailViewModel(
                                orderId = key.orderId,
                                getOrderDetailUseCase = container.getOrderDetailUseCase,
                                resolveShortageUseCase = container.resolveShortageUseCase,
                                cancelOrderUseCase = container.cancelOrderUseCase,
                                deliverOrderUseCase = container.deliverOrderUseCase
                            )
                        }
                    }
                )
                OrderDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navigator.goBack() },
                    onCancelled = { navigator.goBack() }
                )
            }

            entry<NavKeys.SupplierOrderDetail> { key ->
                val vm: SellerOrderDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SellerOrderDetailViewModel(
                                orderId = key.orderId,
                                getOrderDetailUseCase = container.getSupplierOrderDetailUseCase,
                                confirmOrderUseCase = container.confirmSupplierOrderUseCase,
                                reportShortageUseCase = container.reportSupplierOrderShortageUseCase,
                                shipOrderUseCase = container.shipSupplierOrderUseCase,
                                deliverOrderUseCase = container.deliverSupplierOrderUseCase
                            )
                        }
                    }
                )
                SellerOrderDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navigator.goBack() }
                )
            }

            entry<NavKeys.MarketCollection> { key ->
                val vm: MarketCollectionViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            MarketCollectionViewModel(
                                collectionId = key.collectionId,
                                mine = key.mine,
                                getMyCollectionUseCase = container.getMyCollectionUseCase,
                                getBrowseCollectionUseCase = container.getBrowseCollectionUseCase,
                                placeMarketOrderUseCase = container.placeMarketOrderUseCase,
                                setCollectionItemsUseCase = container.setCollectionItemsUseCase,
                                setFeaturedItemsUseCase = container.setFeaturedItemsUseCase,
                                publishCollectionUseCase = container.publishCollectionUseCase,
                                deactivateCollectionUseCase = container.deactivateCollectionUseCase,
                                getMarketInventoryUseCase = container.getMarketInventoryUseCase
                            )
                        }
                    }
                )
                MarketCollectionScreen(
                    viewModel = vm,
                    onNavigateBack = { navigator.goBack() },
                    // The collection is no longer orderable — replace it with the new order, where
                    // the buyer names the collector next.
                    onOrderPlaced = { orderId ->
                        navigator.navigate(
                            NavKeys.MarketOrder(orderId, asSeller = false),
                            popUpTo = Navigator.PopUpTo(key, inclusive = true)
                        )
                    }
                )
            }

            entry<NavKeys.MarketOrder> { key ->
                val vm: MarketOrderViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            MarketOrderViewModel(
                                orderId = key.orderId,
                                asSeller = key.asSeller,
                                getMarketOrderUseCase = container.getMarketOrderUseCase,
                                setCollectorUseCase = container.setCollectorUseCase,
                                cancelMarketPurchaseUseCase = container.cancelMarketPurchaseUseCase,
                                sellerMarketOrderActionUseCase = container.sellerMarketOrderActionUseCase
                            )
                        }
                    }
                )
                MarketOrderScreen(viewModel = vm, onNavigateBack = { navigator.goBack() })
            }
        }
    )
}

// Marketplace notifications are p2p_*-prefixed; which ones exist (and whether they point at an
// order or a collection) isn't specified, so they all open Home's Marketplace tab.
private fun isMarketplaceNotification(type: String) = type.startsWith("p2p_")

private fun resolveNotificationDestination(type: String, notifiableId: String?): NavKeys? =
    when (type) {
        "new_order" -> notifiableId?.let { NavKeys.SupplierOrderDetail(it) }
        "order_confirmed", "shortage_reported", "order_shipped", "order_delivered" -> notifiableId?.let {
            NavKeys.OrderDetail(
                it
            )
        }

        "registration_approved" -> NavKeys.Home
        "registration_declined" -> NavKeys.RegistrationDeclined
        "upload_reminder" -> NavKeys.InventoryUpload
        else -> null
    }

@Composable
private fun OrderSuccessScreen(orderId: String, onNavigateHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SecondaryGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SecondaryGreen,
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Order Placed!",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your order has been placed successfully and will be confirmed shortly.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Order ID: $orderId",
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryBlue,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(40.dp))
        PharmaButton(
            text = "Back to Home",
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// The launcher icon is an adaptive icon, which painterResource can't load — render it to a
// bitmap instead. Used by the logged-out screens (Welcome, Login).
@Composable
private fun rememberAppLogo(): Painter? {
    val context = LocalContext.current
    return remember(context) {
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
            ?.toBitmap(width = 420, height = 420)
            ?.let { BitmapPainter(it.asImageBitmap()) }
    }
}
