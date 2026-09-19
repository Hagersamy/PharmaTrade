package com.pharmatrade.di

import android.content.Context
import com.pharmatrade.core.io.PlatformFileReader
import com.pharmatrade.feature.admin.data.repository.AdminRepositoryImpl
import com.pharmatrade.feature.admin.domain.repository.AdminRepository
import com.pharmatrade.feature.admin.domain.usecase.ApproveRequestUseCase
import com.pharmatrade.feature.admin.domain.usecase.DeclineRequestUseCase
import com.pharmatrade.feature.admin.domain.usecase.GetRegistrationRequestsUseCase
import com.pharmatrade.feature.admin.domain.usecase.GetRegistrationStatsUseCase
import com.pharmatrade.feature.auth.data.repository.AuthRepositoryImpl
import com.pharmatrade.feature.auth.data.repository.ZoneRepositoryImpl
import com.pharmatrade.feature.auth.domain.repository.AuthRepository
import com.pharmatrade.feature.auth.domain.repository.ZoneRepository
import com.pharmatrade.feature.auth.domain.usecase.LoginUseCase
import com.pharmatrade.feature.auth.domain.usecase.LogoutUseCase
import com.pharmatrade.feature.auth.domain.usecase.RegisterUseCase
import com.pharmatrade.feature.cart.data.repository.CartRepositoryImpl
import com.pharmatrade.feature.cart.domain.repository.CartRepository
import com.pharmatrade.feature.cart.domain.usecase.*
import com.pharmatrade.feature.catalog.data.repository.CatalogRepositoryImpl
import com.pharmatrade.feature.catalog.domain.repository.CatalogRepository
import com.pharmatrade.feature.catalog.domain.usecase.GetAllSellersUseCase
import com.pharmatrade.feature.catalog.domain.usecase.GetSellerListingsUseCase
import com.pharmatrade.feature.catalog.domain.usecase.SearchListingsUseCase
import com.pharmatrade.feature.drugs.data.repository.DrugRepositoryImpl
import com.pharmatrade.feature.drugs.domain.repository.DrugRepository
import com.pharmatrade.feature.drugs.domain.usecase.GetDosageFormsUseCase
import com.pharmatrade.feature.drugs.domain.usecase.GetDrugDetailUseCase
import com.pharmatrade.feature.drugs.domain.usecase.GetDrugsUseCase
import com.pharmatrade.feature.drugs.domain.usecase.CreateInventoryItemUseCase
import com.pharmatrade.feature.drugs.domain.usecase.CreateSupplierDrugUseCase
import com.pharmatrade.feature.drugs.domain.usecase.GetInventoryUseCase
import com.pharmatrade.feature.drugs.domain.usecase.GetUploadHistoryUseCase
import com.pharmatrade.feature.drugs.domain.usecase.UpdateInventoryItemUseCase
import com.pharmatrade.feature.drugs.domain.usecase.UploadInventoryUseCase
import com.pharmatrade.feature.notification.data.repository.NotificationRepositoryImpl
import com.pharmatrade.feature.notification.domain.repository.NotificationRepository
import com.pharmatrade.feature.notification.domain.usecase.ClearAllNotificationsUseCase
import com.pharmatrade.feature.notification.domain.usecase.DeleteNotificationUseCase
import com.pharmatrade.feature.notification.domain.usecase.GetNotificationsUseCase
import com.pharmatrade.feature.notification.domain.usecase.GetUnreadNotificationCountUseCase
import com.pharmatrade.feature.notification.domain.usecase.MarkAllNotificationsAsReadUseCase
import com.pharmatrade.feature.notification.domain.usecase.MarkNotificationAsReadUseCase
import com.pharmatrade.feature.pharmacyorder.data.repository.PharmacyOrderRepositoryImpl
import com.pharmatrade.feature.pharmacyorder.domain.repository.PharmacyOrderRepository
import com.pharmatrade.feature.pharmacyorder.domain.usecase.*
import com.pharmatrade.feature.profile.data.repository.ProfileRepositoryImpl
import com.pharmatrade.feature.profile.domain.repository.ProfileRepository
import com.pharmatrade.feature.profile.domain.usecase.ChangePasswordUseCase
import com.pharmatrade.feature.profile.domain.usecase.GetProfileUseCase
import com.pharmatrade.feature.profile.domain.usecase.RequestZoneUpdateUseCase
import com.pharmatrade.feature.profile.domain.usecase.UpdateBranchProfileUseCase
import com.pharmatrade.feature.profile.domain.usecase.UpdateProfileUseCase
import com.pharmatrade.feature.profile.domain.usecase.UpdateSupplierProfileUseCase
import com.pharmatrade.feature.seller.data.repository.SellerRepositoryImpl
import com.pharmatrade.feature.seller.domain.repository.SellerRepository
import com.pharmatrade.feature.seller.domain.usecase.*
import com.pharmatrade.feature.supplierorder.data.repository.SupplierOrderRepositoryImpl
import com.pharmatrade.feature.supplierorder.domain.repository.SupplierOrderRepository
import com.pharmatrade.feature.supplierorder.domain.usecase.*
import com.russhwolf.settings.SharedPreferencesSettings

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    // Repositories (singletons)
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(PlatformFileReader(appContext)) }
    val zoneRepository: ZoneRepository by lazy { ZoneRepositoryImpl() }
    val adminRepository: AdminRepository by lazy { AdminRepositoryImpl() }
    val sellerRepository: SellerRepository by lazy { SellerRepositoryImpl() }
    val catalogRepository: CatalogRepository by lazy { CatalogRepositoryImpl() }
    val cartRepository: CartRepository by lazy {
        val cartPrefs = appContext.getSharedPreferences("pharmatrade_cart", Context.MODE_PRIVATE)
        CartRepositoryImpl(SharedPreferencesSettings(cartPrefs))
    }
    val drugRepository: DrugRepository by lazy { DrugRepositoryImpl(PlatformFileReader(appContext)) }
    val pharmacyOrderRepository: PharmacyOrderRepository by lazy { PharmacyOrderRepositoryImpl(PlatformFileReader(appContext)) }
    val supplierOrderRepository: SupplierOrderRepository by lazy { SupplierOrderRepositoryImpl() }
    val profileRepository: ProfileRepository by lazy { ProfileRepositoryImpl() }
    val notificationRepository: NotificationRepository by lazy { NotificationRepositoryImpl() }

    // Auth use cases
    val loginUseCase by lazy { LoginUseCase(authRepository) }
    val logoutUseCase by lazy { LogoutUseCase(authRepository) }
    val registerUseCase by lazy { RegisterUseCase(authRepository) }

    // Admin use cases
    val getRegistrationStatsUseCase by lazy { GetRegistrationStatsUseCase(adminRepository) }
    val getRegistrationRequestsUseCase by lazy { GetRegistrationRequestsUseCase(adminRepository) }
    val approveRequestUseCase by lazy { ApproveRequestUseCase(adminRepository) }
    val declineRequestUseCase by lazy { DeclineRequestUseCase(adminRepository) }

    // Seller use cases
    val getSellerProfileUseCase by lazy { GetSellerProfileUseCase(sellerRepository) }
    val getSellerListingsUseCase by lazy { GetSellerListingsUseCase(sellerRepository) }
    val updateListingUseCase by lazy { UpdateListingUseCase(sellerRepository) }
    val deleteListingUseCase by lazy { DeleteListingUseCase(sellerRepository) }
    val updateMinOrderUseCase by lazy { UpdateMinimumOrderUseCase(sellerRepository) }

    // Catalog use cases
    val getAllSellersUseCase by lazy { GetAllSellersUseCase(catalogRepository) }
    val getCatalogSellerListingsUseCase by lazy { GetSellerListingsUseCase(catalogRepository) }
    val searchListingsUseCase by lazy { SearchListingsUseCase(catalogRepository) }

    // Drug use cases
    val getDrugsUseCase by lazy { GetDrugsUseCase(drugRepository) }
    val getDrugDetailUseCase by lazy { GetDrugDetailUseCase(drugRepository) }
    val getDosageFormsUseCase by lazy { GetDosageFormsUseCase(drugRepository) }
    val uploadInventoryUseCase by lazy { UploadInventoryUseCase(drugRepository) }
    val getUploadHistoryUseCase by lazy { GetUploadHistoryUseCase(drugRepository) }
    val getInventoryUseCase by lazy { GetInventoryUseCase(drugRepository) }
    val createSupplierDrugUseCase by lazy { CreateSupplierDrugUseCase(drugRepository) }
    val createInventoryItemUseCase by lazy { CreateInventoryItemUseCase(drugRepository) }
    val updateInventoryItemUseCase by lazy { UpdateInventoryItemUseCase(drugRepository) }

    // Cart use cases
    val getCartUseCase by lazy { GetCartUseCase(cartRepository) }
    val addToCartUseCase by lazy { AddToCartUseCase(cartRepository) }
    val updateCartItemUseCase by lazy { UpdateCartItemUseCase(cartRepository) }
    val removeFromCartUseCase by lazy { RemoveFromCartUseCase(cartRepository) }
    val clearCartUseCase by lazy { ClearCartUseCase(cartRepository) }
    val validateCartUseCase by lazy { ValidateCartUseCase() }
    val placeOrderUseCase by lazy { PlaceOrderUseCase(cartRepository) }

    // Pharmacy order use cases
    val getBranchUseCase by lazy { GetBranchUseCase(pharmacyOrderRepository) }
    val getPharmacyOrdersUseCase by lazy { GetPharmacyOrdersUseCase(pharmacyOrderRepository) }
    val getSuppliersUseCase by lazy { GetSuppliersUseCase(pharmacyOrderRepository) }
    val getSupplierInventoryUseCase by lazy { GetSupplierInventoryUseCase(pharmacyOrderRepository) }
    val getAllSuppliersDrugsUseCase by lazy { GetAllSuppliersDrugsUseCase(pharmacyOrderRepository) }
    val createOrderUseCase by lazy { CreateOrderUseCase(pharmacyOrderRepository) }
    val addOrderItemUseCase by lazy { AddOrderItemUseCase(pharmacyOrderRepository) }
    val removeOrderItemUseCase by lazy { RemoveOrderItemUseCase(pharmacyOrderRepository) }
    val uploadOrderItemsUseCase by lazy { UploadOrderItemsUseCase(pharmacyOrderRepository) }
    val allocateOrderUseCase by lazy { AllocateOrderUseCase(pharmacyOrderRepository) }
    val getOrderDetailUseCase by lazy { GetOrderDetailUseCase(pharmacyOrderRepository) }
    val resolveShortageUseCase by lazy { ResolveShortageUseCase(pharmacyOrderRepository) }
    val cancelOrderUseCase by lazy { CancelOrderUseCase(pharmacyOrderRepository) }
    val deliverOrderUseCase by lazy { DeliverOrderUseCase(pharmacyOrderRepository) }

    // Supplier order use cases (seller side)
    val getSupplierOrdersUseCase by lazy { GetSupplierOrdersUseCase(supplierOrderRepository) }
    val getSupplierOrderDetailUseCase by lazy { GetSupplierOrderDetailUseCase(supplierOrderRepository) }
    val confirmSupplierOrderUseCase by lazy { ConfirmSupplierOrderUseCase(supplierOrderRepository) }
    val reportSupplierOrderShortageUseCase by lazy { ReportSupplierOrderShortageUseCase(supplierOrderRepository) }
    val shipSupplierOrderUseCase by lazy { ShipSupplierOrderUseCase(supplierOrderRepository) }
    val deliverSupplierOrderUseCase by lazy { DeliverSupplierOrderUseCase(supplierOrderRepository) }

    // Profile use cases
    val getProfileUseCase by lazy { GetProfileUseCase(profileRepository) }
    val updateProfileUseCase by lazy { UpdateProfileUseCase(profileRepository) }
    val changePasswordUseCase by lazy { ChangePasswordUseCase(profileRepository) }
    val updateSupplierProfileUseCase by lazy { UpdateSupplierProfileUseCase(profileRepository) }
    val updateBranchProfileUseCase by lazy { UpdateBranchProfileUseCase(profileRepository) }
    val requestZoneUpdateUseCase by lazy { RequestZoneUpdateUseCase(profileRepository) }

    // Notification use cases
    val getNotificationsUseCase by lazy { GetNotificationsUseCase(notificationRepository) }
    val getUnreadNotificationCountUseCase by lazy { GetUnreadNotificationCountUseCase(notificationRepository) }
    val markNotificationAsReadUseCase by lazy { MarkNotificationAsReadUseCase(notificationRepository) }
    val markAllNotificationsAsReadUseCase by lazy { MarkAllNotificationsAsReadUseCase(notificationRepository) }
    val deleteNotificationUseCase by lazy { DeleteNotificationUseCase(notificationRepository) }
    val clearAllNotificationsUseCase by lazy { ClearAllNotificationsUseCase(notificationRepository) }
}
