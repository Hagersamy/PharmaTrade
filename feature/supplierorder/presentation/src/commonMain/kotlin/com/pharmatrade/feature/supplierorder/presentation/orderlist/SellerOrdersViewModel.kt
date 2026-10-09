package com.pharmatrade.feature.supplierorder.presentation.orderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderSummary
import com.pharmatrade.feature.supplierorder.domain.usecase.GetSupplierOrdersUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SupplierOrderTab(val label: String, val apiStatus: String?) {
    // GET supplier/orders with no status param returns every status, so "All" is a single call.
    ALL("All", null),
    PENDING("Pending", "pending"),
    // Orders the supplier reported a shortage on — they wait here for the pharmacy to resolve.
    SHORTAGE("Shortage", "partially_available"),
    CONFIRMED("Confirmed", "confirmed"),
    SHIPPED("Shipped", "shipped"),
    DELIVERED("Delivered", "delivered"),
    CANCELLED("Cancelled", "cancelled")
}

data class SellerOrdersUiState(
    val selectedTab: SupplierOrderTab = SupplierOrderTab.ALL,
    val orders: List<SupplierOrderSummary> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SellerOrdersViewModel(
    private val getSupplierOrdersUseCase: GetSupplierOrdersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerOrdersUiState())
    val uiState: StateFlow<SellerOrdersUiState> = _uiState.asStateFlow()

    // No loadOrders() in init{}: this ViewModel is created with Home, so loading here fired
    // supplier/orders on every Home creation even when the Orders tab was never opened.
    // AppNavigation's entry<NavKeys.Home> loads it when the Orders tab is shown, and on Home
    // resume while that tab is showing.

    fun onTabSelected(tab: SupplierOrderTab) {
        if (tab == _uiState.value.selectedTab) return
        // Clear the previous tab's orders so the loading spinner shows immediately instead of
        // leaving stale orders from the old tab on screen until the new ones arrive.
        update { copy(selectedTab = tab, orders = emptyList(), total = 0) }
        loadOrders()
    }

    // The in-flight load and the tab it's for. A second call for the same tab while one is
    // running (Home resume and the Orders tab being shown can both ask at once) is dropped; a
    // different tab cancels the old load so its results can't land over the new tab.
    private var loadJob: Job? = null
    private var loadingTab: SupplierOrderTab? = null

    fun loadOrders() {
        val tab = _uiState.value.selectedTab
        if (loadJob?.isActive == true && loadingTab == tab) return
        loadJob?.cancel()
        loadingTab = tab
        loadJob = viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            when (val result = getSupplierOrdersUseCase(status = tab.apiStatus, perPage = 20)) {
                is Result.Success -> update { copy(isLoading = false, orders = result.data.orders, total = result.data.total) }
                is Result.Error -> update {
                    copy(isLoading = false, error = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun update(block: SellerOrdersUiState.() -> SellerOrdersUiState) {
        _uiState.value = _uiState.value.block()
    }
}
