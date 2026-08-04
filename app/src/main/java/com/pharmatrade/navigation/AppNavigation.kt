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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.pharmatrade.feature.auth.presentation.login.LoginViewModel
import com.pharmatrade.feature.auth.presentation.pending.PendingApprovalScreen
import com.pharmatrade.feature.auth.presentation.register.RegisterScreen
import com.pharmatrade.feature.auth.presentation.register.RegisterViewModel
import com.pharmatrade.feature.cart.presentation.CartScreen
import com.pharmatrade.feature.cart.presentation.CartViewModel
import com.pharmatrade.feature.catalog.presentation.drugs.SellerDrugsScreen
import com.pharmatrade.feature.catalog.presentation.drugs.SellerDrugsViewModel
import com.pharmatrade.feature.home.HomeScreen
import com.pharmatrade.feature.home.HomeTab
import com.pharmatrade.feature.home.NotificationsScreen
import com.pharmatrade.feature.home.ProfileViewModel
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
fun AppNavigation(container: AppContainer) {
    // Restored once per process launch: if SessionManager already holds a persisted session
    // (see SessionManager.init in PharmaTradeApp.onCreate), skip straight past the login screen.
    val startKey = remember {
        when {
            !SessionManager.isLoggedIn -> NavKeys.Login
            SessionManager.user?.isPending == true -> NavKeys.PendingApproval
            SessionManager.user?.userType == UserType.ADMIN -> NavKeys.AdminDashboard
            else -> NavKeys.Home
        }
    }
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(startKey)
    val navigator = remember { Navigator(backStack) }
    val coroutineScope = rememberCoroutineScope()

    // Hoisted above Home's own content so a back press can be intercepted here: the bottom nav
    // bar's tabs (Home/Cart/Orders/Profile) are plain UI state, not separate back-stack entries.
    // This tracks the order tabs were visited in (e.g. Home -> Orders -> Cart -> Profile) so back
    // un-does one tab switch at a time in reverse — Profile -> Cart -> Orders -> Home -> exit —
    // instead of jumping straight from any tab to Home. Re-selecting Home directly resets the
    // history back to just [Home], since Home is this stack's root.
    val tabHistory = rememberSaveable(
        saver = listSaver(save = { it.map(HomeTab::name) }, restore = { it.map(HomeTab::valueOf).toMutableStateList() })
    ) { mutableStateListOf(HomeTab.HOME) }
    val homeSelectedTab = tabHistory.last()
    fun selectHomeTab(tab: HomeTab) {
        if (tab == HomeTab.HOME) {
            tabHistory.clear()
            tabHistory.add(HomeTab.HOME)
        } else if (tab != tabHistory.last()) {
            tabHistory.add(tab)
        }
    }
    BackHandler(enabled = backStack.lastOrNull() == NavKeys.Home && tabHistory.size > 1) {
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

    NavDisplay(
        backStack = backStack,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {

            entry<NavKeys.Login> {
                val vm: LoginViewModel = viewModel(
                    factory = viewModelFactory { initializer { LoginViewModel(container.loginUseCase) } }
                )
                LoginScreen(
                    viewModel = vm,
                    onNavigateToRegister = { navigator.navigate(NavKeys.Register) },
                    onNavigateToSellerDashboard = {
                        navigator.navigate(NavKeys.Home, popUpTo = Navigator.PopUpTo(NavKeys.Login, inclusive = true))
                    },
                    onNavigateToBuyerCatalog = {
                        navigator.navigate(NavKeys.Home, popUpTo = Navigator.PopUpTo(NavKeys.Login, inclusive = true))
                    },
                    onNavigateToAdminDashboard = {
                        navigator.navigate(NavKeys.AdminDashboard, popUpTo = Navigator.PopUpTo(NavKeys.Login, inclusive = true))
                    },
                    onNavigateToPendingApproval = {
                        navigator.navigate(NavKeys.PendingApproval, popUpTo = Navigator.PopUpTo(NavKeys.Login, inclusive = true))
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
                    onNavigateBack = { navigator.goBack() },
                    onNavigateToPendingApproval = {
                        navigator.navigate(NavKeys.PendingApproval, popUpTo = Navigator.PopUpTo(NavKeys.Login, inclusive = true))
                    }
                )
            }

            entry<NavKeys.PendingApproval> {
                val userName = SessionManager.user?.name ?: ""
                PendingApprovalScreen(
                    userName = userName,
                    onBackToLogin = {
                        coroutineScope.launch { container.logoutUseCase() }
                        // Reset the Home tab-visit history so the next login always lands back on
                        // the Home tab, instead of resuming whatever tab (e.g. Profile) was open
                        // when this session logged out.
                        selectHomeTab(HomeTab.HOME)
                        navigator.navigate(NavKeys.Login, popUpTo = Navigator.PopUpTo(target = null))
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
                                declineRequestUseCase = container.declineRequestUseCase
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
                                deactivateAccountUseCase = container.deactivateAccountUseCase,
                                requestZoneUpdateUseCase = container.requestZoneUpdateUseCase,
                                zoneRepository = container.zoneRepository
                            )
                        }
                    }
                )
                AdminDashboardScreen(
                    viewModel = vm,
                    analyticsViewModel = analyticsVm,
                    profileViewModel = adminProfileVm,
                    onNavigateToNotifications = { navigator.navigate(NavKeys.Notifications) },
                    onLogout = {
                        coroutineScope.launch { container.logoutUseCase() }
                        // Reset the Home tab-visit history so the next login always lands back on
                        // the Home tab, instead of resuming whatever tab (e.g. Profile) was open
                        // when this session logged out.
                        selectHomeTab(HomeTab.HOME)
                        navigator.navigate(NavKeys.Login, popUpTo = Navigator.PopUpTo(target = null))
                    }
                )
            }

            entry<NavKeys.Home> {
                val sellerVm: SellerDashboardViewModel = viewModel(
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
                )
                val pharmacyHomeVm: PharmacyHomeViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            PharmacyHomeViewModel(
                                getPharmacyOrdersUseCase = container.getPharmacyOrdersUseCase,
                                getAllSuppliersDrugsUseCase = container.getAllSuppliersDrugsUseCase,
                                getDrugsUseCase = container.getDrugsUseCase,
                                getOrderDetailUseCase = container.getOrderDetailUseCase,
                                createOrderUseCase = container.createOrderUseCase,
                                addOrderItemUseCase = container.addOrderItemUseCase,
                                removeOrderItemUseCase = container.removeOrderItemUseCase
                            )
                        }
                    }
                )
                val orderListVm: OrderListViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { OrderListViewModel(getPharmacyOrdersUseCase = container.getPharmacyOrdersUseCase) }
                    }
                )
                val sellerOrdersVm: SellerOrdersViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { SellerOrdersViewModel(getSupplierOrdersUseCase = container.getSupplierOrdersUseCase) }
                    }
                )
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
                                deactivateAccountUseCase = container.deactivateAccountUseCase,
                                requestZoneUpdateUseCase = container.requestZoneUpdateUseCase,
                                zoneRepository = container.zoneRepository
                            )
                        }
                    }
                )
                // Home's ViewModels are scoped to this back-stack entry and only fetch once in
                // init{}. Without this, returning here from InventoryUpload/AddListing/EditListing/
                // Allocation (which pop back rather than recreating Home) leaves the seller dashboard
                // and the buyer's order list showing stale data — e.g. an order just reviewed and
                // allocated wouldn't show up in the Orders tab until Home was fully recreated,
                // looking to the buyer like the order had been deleted.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            sellerVm.loadData()
                            orderListVm.loadOrders()
                            pharmacyHomeVm.loadActiveOrdersCount()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                HomeScreen(
                    sellerDashboardViewModel = sellerVm,
                    pharmacyHomeViewModel = pharmacyHomeVm,
                    orderListViewModel = orderListVm,
                    sellerOrdersViewModel = sellerOrdersVm,
                    profileViewModel = profileVm,
                    selectedTab = homeSelectedTab,
                    onTabSelected = ::selectHomeTab,
                    onNavigateToNotifications = { navigator.navigate(NavKeys.Notifications) },
                    onNavigateToSearch = {
                        val key = if (SessionManager.currentUser.value?.userType == UserType.SELLER) {
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
                            NavKeys.Checkout(orders.map { (orderId, supplierId) -> OrderSupplierPair(orderId, supplierId) })
                        )
                    },
                    onLogout = {
                        coroutineScope.launch { container.logoutUseCase() }
                        // Reset the Home tab-visit history so the next login always lands back on
                        // the Home tab, instead of resuming whatever tab (e.g. Profile) was open
                        // when this session logged out.
                        selectHomeTab(HomeTab.HOME)
                        navigator.navigate(NavKeys.Login, popUpTo = Navigator.PopUpTo(target = null))
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
                        navigator.navigate(NavKeys.Home, popUpTo = Navigator.PopUpTo(NavKeys.Home, inclusive = true))
                    }
                )
            }

            entry<NavKeys.Notifications> {
                NotificationsScreen(onNavigateBack = { navigator.goBack() })
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
                        navigator.navigate(NavKeys.OrderSuccess(orderId), popUpTo = Navigator.PopUpTo(NavKeys.Home))
                    }
                )
            }

            entry<NavKeys.OrderSuccess> { key ->
                OrderSuccessScreen(
                    orderId = key.orderId,
                    onNavigateHome = {
                        navigator.navigate(NavKeys.Home, popUpTo = Navigator.PopUpTo(NavKeys.Home, inclusive = true))
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
                                getOrderDetailUseCase = container.getOrderDetailUseCase
                            )
                        }
                    }
                )
                OrderItemsScreen(
                    viewModel = vm,
                    onNavigateBack = { navigator.goBack() },
                    onReviewAndAllocate = {
                        navigator.navigate(NavKeys.Allocation(key.orderId, key.supplierId))
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
                        navigator.navigate(NavKeys.OrderDetail(confirmedOrderId), popUpTo = Navigator.PopUpTo(NavKeys.Home))
                    },
                    onCancelled = {
                        navigator.navigate(NavKeys.Home, popUpTo = Navigator.PopUpTo(NavKeys.Home, inclusive = true))
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
        }
    )
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
