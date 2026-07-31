package com.pharmatrade.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()

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

    // Restored once per process launch: if SessionManager already holds a persisted session
    // (see SessionManager.init in PharmaTradeApp.onCreate), skip straight past the login screen.
    val startDestination = remember {
        when {
            !SessionManager.isLoggedIn -> NavRoutes.Login.route
            SessionManager.user?.isPending == true -> NavRoutes.PendingApproval.route
            SessionManager.user?.userType == UserType.ADMIN -> NavRoutes.AdminDashboard.route
            else -> NavRoutes.Home.route
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(NavRoutes.Login.route) {
            val vm: LoginViewModel = viewModel(
                factory = viewModelFactory { initializer { LoginViewModel(container.loginUseCase) } }
            )
            LoginScreen(
                viewModel = vm,
                onNavigateToRegister = { navController.navigate(NavRoutes.Register.route) },
                onNavigateToSellerDashboard = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToBuyerCatalog = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToAdminDashboard = {
                    navController.navigate(NavRoutes.AdminDashboard.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToPendingApproval = {
                    navController.navigate(NavRoutes.PendingApproval.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Register.route) {
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPendingApproval = {
                    navController.navigate(NavRoutes.PendingApproval.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.PendingApproval.route) {
            val userName = SessionManager.user?.name ?: ""
            PendingApprovalScreen(
                userName = userName,
                onBackToLogin = {
                    SessionManager.logout()
                    navController.navigate(NavRoutes.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(NavRoutes.AdminDashboard.route) {
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
                onNavigateToNotifications = { navController.navigate(NavRoutes.Notifications.route) },
                onLogout = {
                    SessionManager.logout()
                    navController.navigate(NavRoutes.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(NavRoutes.Home.route) {
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
                onNavigateToNotifications = { navController.navigate(NavRoutes.Notifications.route) },
                onNavigateToSearch = {
                    val route = if (SessionManager.currentUser.value?.userType == UserType.SELLER) {
                        NavRoutes.SellerSearch.route
                    } else {
                        NavRoutes.BuyerSearch.route
                    }
                    navController.navigate(route)
                },
                onNavigateToUploadInventory = { navController.navigate(NavRoutes.InventoryUpload.route) },
                onNavigateToAddListing = { navController.navigate(NavRoutes.AddListing.route) },
                onNavigateToEditListing = { listingId ->
                    navController.navigate(NavRoutes.EditListing.createRoute(listingId))
                },
                onNavigateToOrderMode = { navController.navigate(NavRoutes.OrderMode.route) },
                onNavigateToOrderDetail = { orderId ->
                    navController.navigate(NavRoutes.OrderDetail.createRoute(orderId))
                },
                onNavigateToSupplierOrderDetail = { orderId ->
                    navController.navigate(NavRoutes.SupplierOrderDetail.createRoute(orderId))
                },
                onCheckoutAll = { orders -> navController.navigate(NavRoutes.Checkout.createRoute(orders)) },
                onLogout = {
                    SessionManager.logout()
                    navController.navigate(NavRoutes.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(
            route = NavRoutes.Checkout.route,
            arguments = listOf(navArgument("orders") { type = NavType.StringType })
        ) { backStack ->
            val encoded = backStack.arguments?.getString("orders") ?: return@composable
            val orders = remember(encoded) { NavRoutes.Checkout.parse(encoded) }
            val vm: CheckoutViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        CheckoutViewModel(
                            orderSupplierPairs = orders,
                            allocateOrderUseCase = container.allocateOrderUseCase,
                            cancelOrderUseCase = container.cancelOrderUseCase
                        )
                    }
                }
            )
            CheckoutScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onDone = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route)
                    }
                }
            )
        }

        composable(NavRoutes.Notifications.route) {
            NotificationsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.BuyerSearch.route) {
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSellerDrugs = { sellerId ->
                    navController.navigate(NavRoutes.SellerDrugs.createRoute(sellerId))
                }
            )
        }

        composable(NavRoutes.SellerSearch.route) {
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { listingId ->
                    navController.navigate(NavRoutes.EditListing.createRoute(listingId))
                },
                onAddListing = { navController.navigate(NavRoutes.AddListing.route) }
            )
        }

        composable(NavRoutes.InventoryUpload.route) {
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
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.AddListing.route) {
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
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.EditListing.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStack ->
            val listingId = backStack.arguments?.getString("listingId") ?: return@composable
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
                editingListingId = listingId,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.SellerDrugs.route,
            arguments = listOf(navArgument("sellerId") { type = NavType.StringType })
        ) { backStack ->
            val sellerId = backStack.arguments?.getString("sellerId") ?: return@composable
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
                sellerId = sellerId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate(NavRoutes.Cart.route) },
                onAddToCart = { listing -> cartViewModel.addToCart(listing) },
                cartItemCount = cartState.cart.getTotalItems()
            )
        }

        composable(NavRoutes.Cart.route) {
            CartScreen(
                viewModel = cartViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOrderSuccess = { orderId ->
                    navController.navigate(NavRoutes.OrderSuccess.createRoute(orderId)) {
                        popUpTo(NavRoutes.Home.route)
                    }
                }
            )
        }

        composable(
            route = NavRoutes.OrderSuccess.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStack ->
            val orderId = backStack.arguments?.getString("orderId") ?: ""
            OrderSuccessScreen(
                orderId = orderId,
                onNavigateHome = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.OrderMode.route) {
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
                onNavigateBack = { navController.popBackStack() },
                onOrderCreated = { orderId, supplierId ->
                    navController.navigate(NavRoutes.OrderItems.createRoute(orderId, supplierId)) {
                        popUpTo(NavRoutes.OrderMode.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.OrderItems.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("supplierId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStack ->
            val orderId = backStack.arguments?.getString("orderId") ?: return@composable
            val supplierId = backStack.arguments?.getString("supplierId")?.takeIf { it.isNotBlank() }
            val vm: OrderItemsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        OrderItemsViewModel(
                            orderId = orderId,
                            supplierId = supplierId,
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
                onNavigateBack = { navController.popBackStack() },
                onReviewAndAllocate = {
                    navController.navigate(NavRoutes.Allocation.createRoute(orderId, supplierId))
                }
            )
        }

        composable(
            route = NavRoutes.Allocation.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("supplierId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStack ->
            val orderId = backStack.arguments?.getString("orderId") ?: return@composable
            val supplierId = backStack.arguments?.getString("supplierId")?.takeIf { it.isNotBlank() }
            val vm: AllocationViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        AllocationViewModel(
                            orderId = orderId,
                            supplierId = supplierId,
                            allocateOrderUseCase = container.allocateOrderUseCase,
                            cancelOrderUseCase = container.cancelOrderUseCase
                        )
                    }
                }
            )
            AllocationScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onConfirmed = { confirmedOrderId ->
                    navController.navigate(NavRoutes.OrderDetail.createRoute(confirmedOrderId)) {
                        popUpTo(NavRoutes.Home.route)
                    }
                },
                onCancelled = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route)
                    }
                }
            )
        }

        composable(
            route = NavRoutes.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStack ->
            val orderId = backStack.arguments?.getString("orderId") ?: return@composable
            val vm: OrderDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        OrderDetailViewModel(
                            orderId = orderId,
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
                onNavigateBack = { navController.popBackStack() },
                onCancelled = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.SupplierOrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStack ->
            val orderId = backStack.arguments?.getString("orderId") ?: return@composable
            val vm: SellerOrderDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        SellerOrderDetailViewModel(
                            orderId = orderId,
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
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
