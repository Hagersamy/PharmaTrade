package com.pharmatrade.feature.supplierorder.presentation.orderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderSummary
import com.pharmatrade.feature.supplierorder.domain.usecase.GetSupplierOrdersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SupplierOrderTab(val label: String, val apiStatus: String?) {
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
        update { copy(selectedTab = tab) }
        loadOrders()
    }

    fun loadOrders() {
        val tab = _uiState.value.selectedTab
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            when (val result = getSupplierOrdersUseCase(status = tab.apiStatus, perPage = 20)) {
                is Result.Success -> update { copy(isLoading = false, orders = result.data.orders, total = result.data.total) }
                is Result.Error -> update { copy(isLoading = false, error = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    private fun update(block: SellerOrdersUiState.() -> SellerOrdersUiState) {
        _uiState.value = _uiState.value.block()
    }
}
