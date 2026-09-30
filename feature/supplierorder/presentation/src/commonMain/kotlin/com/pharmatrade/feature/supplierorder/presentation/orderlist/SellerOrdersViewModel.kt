package com.pharmatrade.feature.supplierorder.presentation.orderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderSummary
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrdersPage
import com.pharmatrade.feature.supplierorder.domain.usecase.GetSupplierOrdersUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SupplierOrderTab(val label: String, val apiStatus: String?) {
    // The backend has no single "return every status" filter — an absent status param defaults
    // to drafts only, and there's no "all" value it recognizes. So "All" fetches every other
    // tab's status in parallel and merges the results client-side instead of a single API call.
    ALL("All", null),
    PENDING("Pending", "pending"),
    CONFIRMED("Confirmed", "confirmed"),
    SHIPPED("Shipped", "shipped"),
    DELIVERED("Delivered", "delivered")
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

    init {
        loadOrders()
    }

    fun onTabSelected(tab: SupplierOrderTab) {
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
            if (tab == SupplierOrderTab.ALL) {
                loadAll()
            } else {
                when (val result = getSupplierOrdersUseCase(status = tab.apiStatus, perPage = 20)) {
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
        val realStatuses = SupplierOrderTab.entries.filter { it != SupplierOrderTab.ALL }.map { it.apiStatus }
        val results = realStatuses.map { status ->
            viewModelScope.async { getSupplierOrdersUseCase(status = status, perPage = 20) }
        }.awaitAll()

        val firstError = results.filterIsInstance<Result.Error>().firstOrNull()
        if (firstError != null && results.none { it is Result.Success }) {
            update { copy(isLoading = false, error = LanguageManager.strings.friendlyError(firstError.message)) }
            return
        }

        val merged = results.filterIsInstance<Result.Success<SupplierOrdersPage>>()
            .flatMap { it.data.orders }
            .sortedByDescending { it.createdAt }
        update { copy(isLoading = false, orders = merged, total = merged.size) }
    }

    private fun update(block: SellerOrdersUiState.() -> SellerOrdersUiState) {
        _uiState.value = _uiState.value.block()
    }
}
