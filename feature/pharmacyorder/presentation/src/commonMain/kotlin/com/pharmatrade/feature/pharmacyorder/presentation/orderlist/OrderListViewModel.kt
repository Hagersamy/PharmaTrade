package com.pharmatrade.feature.pharmacyorder.presentation.orderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.userMessage
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacyOrderSummary
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetPharmacyOrdersUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OrderListTab(val label: String, val apiStatus: String?) {
    // GET pharmacy/orders with no status param returns every status — drafts (the cart's
    // per-supplier draft orders) included — so "All" is a single call with drafts dropped
    // client-side. See OrderListViewModel.loadOrders().
    ALL("All", null),
    PENDING("Pending", "pending_supplier_confirmation"),
    // A supplier reported a shortage and the pharmacy has to cancel or accept the short items.
    SHORTAGE("Shortage", "partially_available"),
    CONFIRMED("Confirmed", "confirmed"),
    SHIPPED("Shipped", "shipped"),
    DELIVERED("Delivered", "delivered"),
    CANCELLED("Cancelled", "cancelled")
}

data class OrderListUiState(
    val selectedTab: OrderListTab = OrderListTab.ALL,
    val orders: List<PharmacyOrderSummary> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class OrderListViewModel(
    private val getPharmacyOrdersUseCase: GetPharmacyOrdersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderListUiState())
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    init {
        // Deliberately does NOT call loadOrders() here — AppNavigation's entry<NavKeys.Home>
        // loads it only while the Orders tab is showing (when the tab is selected, or on Home
        // resume while it's showing), which also covers a just-placed order: checkout returns to
        // Home, and the list reloads the moment the Orders tab is visible.
    }

    fun onTabSelected(tab: OrderListTab) {
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
    private var loadingTab: OrderListTab? = null

    fun loadOrders() {
        val tab = _uiState.value.selectedTab
        if (loadJob?.isActive == true && loadingTab == tab) return
        loadJob?.cancel()
        loadingTab = tab
        loadJob = viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            // "All" asks for a bigger page: the backend has no way to exclude drafts, so they
            // share the page with real orders and are dropped here.
            val isAll = tab == OrderListTab.ALL
            val perPage = if (isAll) ALL_PAGE_SIZE else PAGE_SIZE
            when (val result = getPharmacyOrdersUseCase(status = tab.apiStatus, perPage = perPage)) {
                is Result.Success -> {
                    val orders = if (isAll) {
                        result.data.orders.filter { it.status != DRAFT_STATUS }.sortedByDescending { it.date }
                    } else {
                        result.data.orders
                    }
                    update { copy(isLoading = false, orders = orders, total = if (isAll) orders.size else result.data.total) }
                }
                is Result.Error -> update {
                    copy(isLoading = false, error = LanguageManager.strings.userMessage(result))
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun update(block: OrderListUiState.() -> OrderListUiState) {
        _uiState.value = _uiState.value.block()
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val ALL_PAGE_SIZE = 100
        const val DRAFT_STATUS = "draft"
    }
}
