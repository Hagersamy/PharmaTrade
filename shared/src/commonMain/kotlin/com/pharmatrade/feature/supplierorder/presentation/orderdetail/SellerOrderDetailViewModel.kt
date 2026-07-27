package com.pharmatrade.feature.supplierorder.presentation.orderdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.supplierorder.domain.model.ConfirmItemInput
import com.pharmatrade.feature.supplierorder.domain.model.ShortageItemInput
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderDetail
import com.pharmatrade.feature.supplierorder.domain.usecase.ConfirmSupplierOrderUseCase
import com.pharmatrade.feature.supplierorder.domain.usecase.DeliverSupplierOrderUseCase
import com.pharmatrade.feature.supplierorder.domain.usecase.GetSupplierOrderDetailUseCase
import com.pharmatrade.feature.supplierorder.domain.usecase.ReportSupplierOrderShortageUseCase
import com.pharmatrade.feature.supplierorder.domain.usecase.ShipSupplierOrderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SellerOrderDetailUiState(
    val isLoading: Boolean = false,
    val order: SupplierOrderDetail? = null,
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val actionError: String? = null,
    val snackbarMessage: String? = null,
    val showConfirmDialog: Boolean = false,
    val showShortageDialog: Boolean = false,
    val confirmQuantities: Map<String, String> = emptyMap(),
    val shortageQuantities: Map<String, String> = emptyMap(),
    val shortageNotes: String = ""
)

class SellerOrderDetailViewModel(
    private val orderId: String,
    private val getOrderDetailUseCase: GetSupplierOrderDetailUseCase,
    private val confirmOrderUseCase: ConfirmSupplierOrderUseCase,
    private val reportShortageUseCase: ReportSupplierOrderShortageUseCase,
    private val shipOrderUseCase: ShipSupplierOrderUseCase,
    private val deliverOrderUseCase: DeliverSupplierOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerOrderDetailUiState())
    val uiState: StateFlow<SellerOrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            when (val result = getOrderDetailUseCase(orderId)) {
                is Result.Success -> update {
                    copy(
                        isLoading = false,
                        order = result.data,
                        confirmQuantities = result.data.items.associate { it.id to it.quantityRequested.toString() },
                        shortageQuantities = result.data.items.associate { it.id to it.quantityRequested.toString() }
                    )
                }
                is Result.Error -> update { copy(isLoading = false, error = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun openConfirmDialog() = update { copy(showConfirmDialog = true, actionError = null) }
    fun dismissConfirmDialog() = update { copy(showConfirmDialog = false) }

    fun onConfirmQuantityChange(itemId: String, value: String) {
        if (value.isNotEmpty() && value.toIntOrNull() == null) return
        update { copy(confirmQuantities = confirmQuantities + (itemId to value)) }
    }

    fun submitConfirm() {
        val order = _uiState.value.order ?: return
        val items = order.items.map { item ->
            val qty = _uiState.value.confirmQuantities[item.id]?.toIntOrNull() ?: item.quantityRequested
            ConfirmItemInput(orderItemId = item.id, quantityConfirmed = qty)
        }
        viewModelScope.launch {
            update { copy(isSubmitting = true, actionError = null) }
            when (val result = confirmOrderUseCase(orderId, items)) {
                is Result.Success -> {
                    update { copy(isSubmitting = false, showConfirmDialog = false, snackbarMessage = "Order confirmed") }
                    loadOrder()
                }
                is Result.Error -> update { copy(isSubmitting = false, actionError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun openShortageDialog() = update { copy(showShortageDialog = true, actionError = null) }
    fun dismissShortageDialog() = update { copy(showShortageDialog = false) }

    fun onShortageQuantityChange(itemId: String, value: String) {
        if (value.isNotEmpty() && value.toIntOrNull() == null) return
        update { copy(shortageQuantities = shortageQuantities + (itemId to value)) }
    }

    fun onShortageNotesChange(value: String) = update { copy(shortageNotes = value) }

    fun submitShortage() {
        val order = _uiState.value.order ?: return
        val items = order.items.map { item ->
            val available = (_uiState.value.shortageQuantities[item.id]?.toIntOrNull() ?: item.quantityRequested)
                .coerceIn(0, item.quantityRequested)
            val short = item.quantityRequested - available
            ShortageItemInput(
                orderItemId = item.id,
                quantityConfirmed = available,
                quantityShort = short,
                notes = _uiState.value.shortageNotes.takeIf { it.isNotBlank() }
            )
        }
        viewModelScope.launch {
            update { copy(isSubmitting = true, actionError = null) }
            when (val result = reportShortageUseCase(orderId, items)) {
                is Result.Success -> {
                    update { copy(isSubmitting = false, showShortageDialog = false, snackbarMessage = "Shortage reported") }
                    loadOrder()
                }
                is Result.Error -> update { copy(isSubmitting = false, actionError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun ship() {
        viewModelScope.launch {
            update { copy(isSubmitting = true, actionError = null) }
            when (val result = shipOrderUseCase(orderId)) {
                is Result.Success -> {
                    update { copy(isSubmitting = false, snackbarMessage = "Order marked as shipped") }
                    loadOrder()
                }
                is Result.Error -> update { copy(isSubmitting = false, actionError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun deliver() {
        viewModelScope.launch {
            update { copy(isSubmitting = true, actionError = null) }
            when (val result = deliverOrderUseCase(orderId)) {
                is Result.Success -> {
                    update { copy(isSubmitting = false, snackbarMessage = "Order marked as delivered") }
                    loadOrder()
                }
                is Result.Error -> update { copy(isSubmitting = false, actionError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun onSnackbarShown() = update { copy(snackbarMessage = null) }

    private fun update(block: SellerOrderDetailUiState.() -> SellerOrderDetailUiState) {
        _uiState.value = _uiState.value.block()
    }
}
