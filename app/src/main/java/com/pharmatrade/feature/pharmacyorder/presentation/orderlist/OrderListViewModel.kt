package com.pharmatrade.feature.pharmacyorder.presentation.orderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacyOrderSummary
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetPharmacyOrdersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OrderListTab(val label: String, val apiStatus: String?) {
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
        loadOrders()
    }

    fun onTabSelected(tab: OrderListTab) {
        if (tab == _uiState.value.selectedTab) return
        update { copy(selectedTab = tab) }
        loadOrders()
    }

    fun loadOrders() {
        val tab = _uiState.value.selectedTab
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            when (val result = getPharmacyOrdersUseCase(status = tab.apiStatus, perPage = 20)) {
                is Result.Success -> update { copy(isLoading = false, orders = result.data.orders, total = result.data.total) }
                is Result.Error -> update { copy(isLoading = false, error = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    private fun update(block: OrderListUiState.() -> OrderListUiState) {
        _uiState.value = _uiState.value.block()
    }
}
