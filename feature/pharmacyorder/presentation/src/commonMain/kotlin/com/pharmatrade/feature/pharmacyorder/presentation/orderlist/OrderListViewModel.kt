package com.pharmatrade.feature.pharmacyorder.presentation.orderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.pharmacyorder.domain.PharmacyCartBus
import com.pharmatrade.feature.pharmacyorder.domain.model.OrdersPage
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacyOrderSummary
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetPharmacyOrdersUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class OrderListTab(val label: String, val apiStatus: String?) {
    // The backend has no single "return every status" filter — an absent status param defaults
    // to drafts only, and there's no "all" value it recognizes. So "All" fetches every other
    // tab's status in parallel and merges the results client-side instead of a single API call.
    ALL("All", null),
    PENDING("Pending", "pending_supplier_confirmation"),
    CONFIRMED("Confirmed", "confirmed"),
    SHIPPED("Shipped", "shipped"),
    DELIVERED("Delivered", "delivered")
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
        // lifecycle observer calls it on ON_RESUME, which fires immediately (synchronous catch-up,
        // since the Activity is already RESUMED when this VM is constructed) and covers first load
        // on its own. Calling it here too used to double every one of the (up to 4, for "All")
        // requests this fires on startup.

        // Fires when a draft order is confirmed or cancelled (checkout/allocation), which runs on
        // a separate back-stack entry with no direct reference back to this ViewModel — reload so
        // a just-placed order shows up here without waiting for this screen to be recreated.
        PharmacyCartBus.supplierOrderResolved
            .onEach { loadOrders() }
            .launchIn(viewModelScope)
    }

    fun onTabSelected(tab: OrderListTab) {
        if (tab == _uiState.value.selectedTab) return
        // Clear the previous tab's orders so the loading spinner shows immediately instead of
        // leaving stale orders from the old tab on screen until the new ones arrive.
        update { copy(selectedTab = tab, orders = emptyList(), total = 0) }
        loadOrders()
    }

    fun loadOrders() {
        val tab = _uiState.value.selectedTab
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            if (tab == OrderListTab.ALL) {
                loadAll()
            } else {
                when (val result = getPharmacyOrdersUseCase(status = tab.apiStatus, perPage = 20)) {
                    is Result.Success -> update { copy(isLoading = false, orders = result.data.orders, total = result.data.total) }
                    is Result.Error -> update {
                        copy(isLoading = false, error = LanguageManager.strings.friendlyError(result.message))
                    }
                    is Result.Loading -> Unit
                }
            }
        }
    }

    private suspend fun loadAll() {
        val realStatuses = OrderListTab.entries.filter { it != OrderListTab.ALL }.map { it.apiStatus }
        val results = realStatuses.map { status ->
            viewModelScope.async { getPharmacyOrdersUseCase(status = status, perPage = 20) }
        }.awaitAll()

        val firstError = results.filterIsInstance<Result.Error>().firstOrNull()
        if (firstError != null && results.none { it is Result.Success }) {
            update { copy(isLoading = false, error = LanguageManager.strings.friendlyError(firstError.message)) }
            return
        }

        val merged = results.filterIsInstance<Result.Success<OrdersPage>>()
            .flatMap { it.data.orders }
            .sortedByDescending { it.date }
        update { copy(isLoading = false, orders = merged, total = merged.size) }
    }

    private fun update(block: OrderListUiState.() -> OrderListUiState) {
        _uiState.value = _uiState.value.block()
    }
}
