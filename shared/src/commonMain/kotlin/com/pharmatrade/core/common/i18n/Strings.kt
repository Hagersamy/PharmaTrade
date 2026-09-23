package com.pharmatrade.core.common.i18n

// One key per user-facing UI string. Grouped by feature with comment headers since this
// grows large (~350+ across the app) — add new sections as more screens are localized.
// Backend-sourced text (Result.Error messages) is never shown to the user verbatim — it's
// classified into one of the errorXxx keys below via Strings.friendlyError() (see
// core/common/error/ErrorMessages.kt) so raw exception/DB text never reaches a toast/snackbar.
interface Strings {
    // ── Errors (generic, localized; raw backend/exception text never shown as-is) ──
    val errorGeneric: String
    val errorNetwork: String
    val errorInvalidCredentials: String
    val errorEmailInUse: String
    val errorInvalidPhone: String
    val errorInvalidPrice: String
    val errorInvalidDiscount: String
    val errorInvalidQuantity: String
    val errorSelectDrug: String
    val errorCartEmpty: String
    val errorInvalidMinOrderValue: String
    val errorInvalidMinOrderQty: String
    val errorStillProcessingUpload: String
    fun errorOnlyUnitsAvailable(quantity: Int, drugName: String): String
    val errorNameRequired: String
    val errorInvalidEmail: String
    val errorPhoneRequired: String
    val errorPasswordTooShort: String
    val errorBusinessNameRequired: String
    val errorLicenceNumberRequired: String
    val errorZoneRequired: String
    val errorAddressRequired: String
    val errorLicenceFrontRequired: String
    val errorLicenceBackRequired: String
    val errorNoFileSelected: String
    val errorDrugNameRequired: String
    val errorTradeNameRequired: String

    // ── Common ──────────────────────────────────────────────────────────────
    val commonRetry: String
    val commonCancel: String
    val commonSave: String
    val commonSubmit: String
    val commonBack: String
    val commonClearAll: String
    val commonClear: String
    val commonDelete: String
    val commonRemove: String

    // ── Bottom navigation / top bar chrome ─────────────────────────────────
    val navHome: String
    val navCart: String
    val navOrders: String
    val navProfile: String
    val navRequests: String
    val navAnalytics: String
    val appTitle: String
    val homeTaglineSeller: String
    val homeTaglineBuyer: String
    val notificationsTitle: String
    val adminPanel: String
    val homeCreateOrderContentDescription: String

    // ── Login ───────────────────────────────────────────────────────────────
    val loginTagline: String
    val loginWelcomeBack: String
    val loginSubtitle: String
    val loginPhoneLabel: String
    val loginPasswordLabel: String
    val loginSignIn: String
    val loginNoAccount: String
    val loginRegister: String

    // ── Pending approval / registration outcome ────────────────────────────
    val pendingSubmittedTitle: String
    fun pendingGreeting(name: String): String
    val pendingBodyMessage: String
    val pendingWhatsNextTitle: String
    val pendingWhatsNextSteps: String
    val declinedTitle: String
    val declinedBodyMessage: String
    val backToLogin: String

    // ── Notifications ───────────────────────────────────────────────────────
    val notificationsEmptyTitle: String
    val notificationsEmptyMessage: String
    val notificationsDeleteContentDescription: String

    // ── Profile ─────────────────────────────────────────────────────────────
    val profileAccountInfo: String
    val profileEditProfile: String
    val profileEmail: String
    val profilePhone: String
    val profileBusinessName: String
    val profileLicenceNumber: String
    val profileAddress: String
    val profileChangePassword: String
    val profileSupplierInfo: String
    val profileEditSupplierInfo: String
    val profileMinOrderValue: String
    val profileMinOrderQty: String
    val profileDeliveryZones: String
    val profilePrimaryZone: String
    val profileRequestZoneChange: String
    val profileBranchLocation: String
    val profileEditBranchInfo: String
    val profileBranchLoadError: String
    val profileBranchName: String
    val profileSignOut: String
    val profileLanguage: String
    val profileLanguageEnglish: String
    val profileLanguageArabic: String

    // Profile dialogs
    val profileDialogEditProfileTitle: String
    val profileDialogName: String
    val profileDialogPasswordConfirm: String
    val profileDialogChangePasswordTitle: String
    val profileDialogCurrentPassword: String
    val profileDialogNewPassword: String
    val profileDialogConfirmNewPassword: String
    val profileDialogEditSupplierTitle: String
    val profileDialogMinOrderValueEgp: String
    val profileDialogMinOrderQty: String
    val profileDialogEditBranchTitle: String
    val profileDialogBranchName: String
    val profileDialogLicenceNumber: String
    val profileDialogRequestZoneTitle: String
    val profileDialogRequestZoneBody: String
    fun profileDialogZonesLoadError(error: String): String
    val profileDialogReason: String

    // ── Cart ────────────────────────────────────────────────────────────────
    fun cartTitle(itemCount: Int): String
    val cartEmptyTitle: String
    val cartEmptyMessage: String
    val cartBrowseSellers: String
    val cartOrderTotal: String
    val cartPlaceOrder: String
    val cartResolveMinimumWarning: String
    fun cartMinOrder(formattedAmount: String): String

    // ── Catalog / seller browse ─────────────────────────────────────────────
    val catalogBrowseDrugsSellers: String
    val catalogSearchPlaceholder: String
    val catalogSearchSellersPlaceholder: String
    val catalogLogoutContentDescription: String
    val catalogNoSellersFound: String
    val catalogNoSellersMessage: String
    fun catalogAvailableSellers(count: Int): String
    fun catalogCompletedOrders(count: Int): String
    val catalogBrowse: String
    val catalogNoResults: String
    val catalogNoResultsMessage: String
    fun catalogSearchResults(count: Int): String
    val catalogViewSeller: String
    fun catalogAddedToCart(drugName: String): String
    val commonLoading: String
    fun catalogMinimumOrder(formattedAmount: String): String
    val catalogNoListings: String
    val catalogNoListingsMessage: String
    val catalogFilterAll: String
    fun catalogDrugsAvailable(count: Int): String
    fun catalogUnitsAvailableExp(quantity: Int, expiryDate: String): String
    fun catalogWasPrice(formattedAmount: String): String
    val catalogAdd: String
    fun catalogPerUnit(unit: String): String

    // ── Order status ────────────────────────────────────────────────────────
    val statusDraft: String
    val statusPending: String
    val statusConfirmed: String
    val statusShipped: String
    val statusDelivered: String
    val statusShortage: String
    val statusCancelled: String

    // ── Pharmacy home / catalog browse ─────────────────────────────────────
    val pharmacyAllSuppliersDrugs: String
    val pharmacySearchDrugName: String
    val commonDismiss: String
    val pharmacyNoDrugsAvailable: String
    val pharmacyCheckBackLater: String
    val pharmacyNoMatches: String
    val pharmacyTryDifferentSearch: String
    val pharmacyOutOfStock: String
    fun pharmacyOnlyLeft(quantity: Int): String
    fun pharmacyInStock(quantity: Int): String
    val commonAddToCart: String
    val commonDecreaseQuantity: String
    val commonIncreaseQuantity: String
    fun pharmacyFromSupplierPriceStock(supplierName: String, formattedPrice: String, quantity: Int): String
    val pharmacyMaxAvailableReached: String
    val catalogPublicPriceLabel: String
    val catalogPharmacistPriceLabel: String
    val tabAll: String
    val orderNeedsAttention: String
    val orderListEmptyTitle: String
    val orderListEmptyMessage: String
    val supplierOrderListEmptyTitle: String

    // ── Order detail ────────────────────────────────────────────────────────
    val orderDetailsTitle: String
    val commonTotal: String
    val orderConfirmDelivery: String
    val orderCancelOrder: String
    fun orderShortageItemCount(count: Int): String
    val orderShortageSomeItems: String
    fun orderShortageReportedMessage(itemsText: String): String
    val orderCancelMissingItems: String
    val orderWaitForAlternatives: String
    val commonCollapse: String
    val commonExpand: String
    fun orderRequestedConfirmed(requested: Int, confirmed: String): String
    fun orderPriceToTotal(unitPrice: String, lineTotal: String): String

    // ── Seller order detail ─────────────────────────────────────────────────
    fun sellerOrderConfirmedAt(formatted: String): String
    fun sellerOrderShippedAt(formatted: String): String
    fun sellerOrderDeliveredAt(formatted: String): String
    val commonSubtotal: String
    fun sellerOrderPlatformCommission(percent: String, formattedAmount: String): String
    val commonPharmacy: String
    val sellerOrderConfirmOrder: String
    val sellerOrderReportShortage: String
    val sellerOrderMarkShipped: String
    val sellerOrderMarkDelivered: String
    val sellerOrderConfirmDialogInstructions: String
    fun sellerOrderConfirmedQtyLabel(requested: Int): String
    val commonConfirm: String
    val sellerOrderShortageDialogInstructions: String
    fun sellerOrderAvailableQtyLabel(requested: Int): String
    val sellerOrderNotesLabel: String

    // ── Order mode ──────────────────────────────────────────────────────────
    val orderModeTitle: String
    val orderModeHowToOrder: String
    val orderModeBestDiscountTitle: String
    val orderModeBestDiscountDesc: String
    val orderModeSpecificSupplierTitle: String
    val orderModeSpecificSupplierDesc: String
    val orderModeNotesLabel: String
    val orderModeNotesPlaceholder: String
    val commonContinue: String
    val orderModeLoadingSuppliers: String
    val orderModeSupplierRequired: String
    val orderModeSelectSupplier: String
    fun orderModeSupplierMinOrder(formattedAmount: String): String
    fun orderModeSupplierItemCount(count: Int): String
    val orderModeCouldNotLoadSuppliers: String

    // ── Allocation ──────────────────────────────────────────────────────────
    val allocationTitle: String
    val allocationFindingBestPrices: String
    fun allocationYouSaved(formattedAmount: String): String
    val allocationConfirmOrder: String
    fun allocationQtyPrice(quantity: Int, formattedUnitPrice: String): String

    // ── Checkout ────────────────────────────────────────────────────────────
    val checkoutTitle: String
    val checkoutEmptyTitle: String
    val checkoutEmptyMessage: String
    fun checkoutAcrossOrders(count: Int): String
    val commonDone: String
    val checkoutSubmittingOrder: String
    val checkoutRemoveOrderContentDescription: String

    // ── Pharmacy cart (Home "Cart" tab) ─────────────────────────────────────
    val pharmacyCartTitle: String
    val pharmacyCartEmptyTitle: String
    val pharmacyCartEmptyMessage: String
    fun pharmacyCartItemsOrdersSummary(itemCount: Int, orderCount: Int): String
    fun pharmacyCartCheckoutAll(orderCount: Int): String
    val pharmacyCartRemoveOrder: String
    fun pharmacyCartLineTotal(formattedUnitPrice: String, quantity: Int, formattedTotal: String): String

    // ── Order items (add-items flow) ────────────────────────────────────────
    val oiSupplierInventoryFallback: String
    val oiAddItemsTitle: String
    fun oiItemsAddedMessage(count: Int): String
    fun oiItemsSkippedSuffix(count: Int): String
    fun oiFilterSupplierDrugs(supplierName: String): String
    val oiFilterDrugsGeneric: String
    fun oiDrugsCountOfTotal(shown: Int, total: Int): String
    val oiNoInventoryTitle: String
    val oiNoInventoryMessage: String
    fun oiDrugsAvailableCount(count: Int): String
    val oiMinimumOrderLabel: String
    fun oiMinOrderProgress(current: String, minimum: String): String
    val oiUploadExcelCsvContentDescription: String
    fun oiAddedToOrder(count: Int): String
    val oiQuickAddContentDescription: String
    val oiReviewAndAllocate: String
    val oiSearchDrugsLabel: String
    val oiUploading: String
    val oiUploadExcelCsv: String
    val oiNoItemsYetTitle: String
    val oiNoItemsYetMessage: String
    fun oiInThisOrder(count: Int): String
    fun oiQtyLabel(quantity: Int): String
    fun oiRemoveItemContentDescription(drugName: String): String
    val oiQuantityLabel: String

    // ── Register ────────────────────────────────────────────────────────────
    val regCreateAccount: String
    val regIAmA: String
    val regAccountDetails: String
    val regFullName: String
    val regBusinessCompanyName: String
    val regPharmacyName: String
    val regEmailAddress: String
    val regConfirmPassword: String
    val regPasswordsDoNotMatch: String
    val regSupplierDetails: String
    val regPharmacyDetails: String
    val regLicenceImages: String
    val regFrontOfLicence: String
    val regBackOfLicence: String
    val regOrderSettings: String
    val regPharmacyLicenseContentDescription: String
    val regRemoveImageContentDescription: String
    val regTapToUpload: String
    val regJpgPngAccepted: String
    val regLoadingZones: String
    val regZonesRequired: String
    val regSelectZones: String
    val regOtherGovernorate: String
    fun regRemoveZoneContentDescription(zoneName: String): String
    val regCouldNotLoadZones: String
    val regSellerAgent: String
    val regDistributeDrugs: String
    val regBuyDrugs: String

    // ── Search ──────────────────────────────────────────────────────────────
    val searchSellersDrugsPlaceholder: String
    val searchSellersLabel: String
    val searchDrugsLabel: String
    val searchSortDrugsContentDescription: String
    val searchBestPrice: String
    val searchAZ: String
    val searchTypeToSearchHint: String
    fun searchNoResultsMessage(query: String): String
    fun searchSellersCount(count: Int): String
    val searchNoSellersMatched: String
    val searchDrugsSearching: String
    fun searchDrugsCount(count: Int): String
    val searchNoDrugsMatched: String

    // ── Seller search (own catalog) ─────────────────────────────────────────
    val sellerSearchCatalogPlaceholder: String
    fun sellerSearchCatalogResultsCount(count: Int): String
    fun sellerSearchAlreadyListed(formattedPrice: String): String
    val sellerSearchEditListingContentDescription: String
    val sellerSearchAddAsListingContentDescription: String
    fun sellerSearchAllListingsCount(count: Int): String
    val sellerSearchNoListingsTitle: String
    val sellerSearchNoListingsMessage: String
    fun sellerSearchPriceQty(formattedPrice: String, quantity: Int, unit: String): String
    fun sellerSearchNoResultsMessage(query: String): String

    // ── Seller dashboard ────────────────────────────────────────────────────
    val sdAddListingContentDescription: String
    val sdSellerDashboardTitle: String
    val sdStatListings: String
    val sdStatMinOrder: String
    val sdStatRating: String
    val sdSearchListingsPlaceholder: String
    val sdUploadInventoryContentDescription: String
    val sdNoListingsYetTitle: String
    val sdNoListingsYetMessage: String
    val sdUploadExcelSheet: String
    fun sdMyDrugListings(count: Int): String
    val sdMinOrderAmountTitle: String
    val sdMinOrderAmountBody: String
    val sdAmountEgpLabel: String
    val sdEgpPrefix: String
    val sdEditInventoryItemTitle: String
    val sdDrugNameLabel: String
    val sdQuantityAvailableLabel: String
    val sdUnitPriceEgpLabel: String
    val sdDiscountPctLabel: String
    val sdEditContentDescription: String
    val sdStock: String
    val sdDiscount: String
    val sdPublicPrice: String
    val sdPharmacistPrice: String
    val sdEffectivePriceLabel: String
    fun sdUpdatedAt(formatted: String): String

    // ── Inventory upload ────────────────────────────────────────────────────
    val iuBulkImport: String
    val iuUploadInventoryTitle: String
    val iuFileTypesBadge: String
    val iuImportDrugCatalogue: String
    val iuImportBannerDesc: String
    val iuTapToChangeFile: String
    val iuTapToSelectFile: String
    val iuFileTypesHint: String
    val iuUploadingLabel: String
    val iuPleaseWait: String
    val iuSomethingWentWrong: String
    val iuProcessingYourFile: String
    val iuProcessingDesc: String
    val iuImportComplete: String
    fun iuRowsImported(count: Int): String
    fun iuRowsFailedSuffix(count: Int): String
    val iuSuccessfullySuffix: String
    val iuCheckInventoryHome: String
    val iuImportFailed: String
    val iuImportFailedDesc: String
    val iuSheetFormatGuide: String
    val iuRequiredColumns: String
    val iuOptionalColumns: String
    val iuColDrugName: String
    val iuColPublicPrice: String
    val iuColPharmacistPrice: String
    val iuColDiscount: String
    val iuColQuantity: String
    val iuColLimit: String
    val iuLastUpload: String
    val iuStatusCompleted: String
    val iuStatusFailed: String
    val iuStatusProcessing: String
    val iuRefreshStatusContentDescription: String
    val iuTotalRows: String
    val iuImported: String
    val iuFailed: String

    // ── Drug form ───────────────────────────────────────────────────────────
    val dfEditListingTitle: String
    val dfAddDrugListingTitle: String
    val dfSelectDrug: String
    val dfPricingAvailability: String
    val dfPricePerUnit: String
    val dfDiscountPercentage: String
    val dfCustomerPays: String
    val dfQuantityAvailable: String
    val dfUnit: String
    val dfExpiryDate: String
    val dfUpdateListing: String
    val dfAddListing: String
    val dfAddNewDrugTitle: String
    val dfAddNewDrugDesc: String
    val dfDrugNameRequired: String
    val dfTradeNameRequired: String
    val dfScientificName: String
    val dfManufacturer: String
    val dfDosageForm: String
    val dfStrength: String
    val dfBarcodeOptional: String
    val dfAddDrugButton: String
    val dfCancel: String
    val dfDrugFieldRequired: String
    val dfTypeToSearchDrugs: String
    val dfCantFindDrugAddIt: String
    val dfCouldNotLoadDrugs: String
    val dfRetry: String

    // ── Admin dashboard ─────────────────────────────────────────────────────
    val commonSupplier: String
    val adDashboardTitle: String
    val adRegistrationRequestsTitle: String
    val adPharmaciesTab: String
    val adSuppliersTab: String
    val adAllClearTitle: String
    val adNoPendingPharmacies: String
    val adNoPendingSuppliers: String
    val adRefresh: String
    val adWelcomeBack: String
    val adAdminFallbackName: String
    val adTotalRegistered: String
    val adPendingApproval: String
    val adApprovedThisMonth: String
    val adActiveUsers: String
    val adPendingApprovalsSection: String
    val adPendingLabel: String
    val adRecentRegistrationsSection: String
    val adRejectRegistrationTitle: String
    fun adRejectReasonPrompt(name: String): String
    val adReasonRequiredLabel: String
    val adReasonPlaceholder: String
    val adReasonRequiredError: String
    val adReject: String
    val adApprove: String
    val adFrontLabel: String
    val adBackLabel: String
    val adAccountDetailsSection: String
    val adFullName: String
    val adLicenceNo: String
    val adZoneId: String
    val adLicenceImagesSection: String
    val adSupplierDetailsSection: String
    val adDeliveryZones: String
    val adFailedToLoad: String
    val adNotUploaded: String
    fun adLicenceContentDescription(label: String): String

    // ── Admin analytics ─────────────────────────────────────────────────────
    val aaRegistrationOverview: String
    val aaApproved: String
    val aaDeclined: String
    val aaTotal: String
    val aaPendingPharmacies: String
    val aaPendingSuppliers: String
    val aaPendingRequestsSection: String
    val aaNoPendingRequestsTitle: String
    val aaNoPendingRequestsMessage: String
    val aaLicenceLabel: String
    val aaZoneLabel: String
    val aaMinOrderLabel: String
    val aaExtraZonesLabel: String
    val aaDecline: String
    fun aaApproveDialogTitle(name: String): String
    val aaApproveDialogDesc: String
    val aaNotesOptionalLabel: String
    val aaNotesPlaceholder: String
    fun aaDeclineDialogTitle(name: String): String
    val aaDeclineDialogDesc: String

    // ── Minimum order warning (shared component) ───────────────────────────
    fun minOrderNotMetTitle(sellerName: String): String
    fun minOrderCurrentVsMinimum(current: String, minimum: String): String
    fun minOrderAddMoreFromSeller(amount: String): String
}
