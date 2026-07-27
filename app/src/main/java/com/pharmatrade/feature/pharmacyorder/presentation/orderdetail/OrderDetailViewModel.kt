package com.pharmatrade.feature.pharmacyorder.presentation.orderdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderDetail
import com.pharmatrade.feature.pharmacyorder.domain.usecase.CancelOrderUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.DeliverOrderUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetOrderDetailUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.ResolveShortageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val order: OrderDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isResolvingShortage: Boolean = false,
    val isCancelling: Boolean = false,
    val isDelivering: Boolean = false,
    val actionError: String? = null,
    val cancelled: Boolean = false
)

class OrderDetailViewModel(
    private val orderId: String,
    private val getOrderDetailUseCase: GetOrderDetailUseCase,
    private val resolveShortageUseCase: ResolveShortageUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val deliverOrderUseCase: DeliverOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            when (val result = getOrderDetailUseCase(orderId)) {
                is Result.Success -> update { copy(isLoading = false, order = result.data) }
                is Result.Error -> update { copy(isLoading = false, error = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun resolveShortage(action: String) {
        val shortageReportIds = _uiState.value.order?.shortageReportIds ?: emptyList()
        viewModelScope.launch {
            update { copy(isResolvingShortage = true, actionError = null) }
            when (val result = resolveShortageUseCase(orderId, action, shortageReportIds)) {
                is Result.Success -> {
                    update { copy(isResolvingShortage = false) }
                    loadOrder()
                }
                is Result.Error -> update { copy(isResolvingShortage = false, actionError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun cancelOrder() {
        viewModelScope.launch {
            update { copy(isCancelling = true, actionError = null) }
            when (val result = cancelOrderUseCase(orderId)) {
                is Result.Success -> update { copy(isCancelling = false, cancelled = true) }
                is Result.Error -> update { copy(isCancelling = false, actionError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun deliver() {
        viewModelScope.launch {
            update { copy(isDelivering = true, actionError = null) }
            when (val result = deliverOrderUseCase(orderId)) {
                is Result.Success -> {
                    update { copy(isDelivering = false) }
                    loadOrder()
                }
                is Result.Error -> update { copy(isDelivering = false, actionError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    private fun update(block: OrderDetailUiState.() -> OrderDetailUiState) {
        _uiState.value = _uiState.value.block()
    }
}
