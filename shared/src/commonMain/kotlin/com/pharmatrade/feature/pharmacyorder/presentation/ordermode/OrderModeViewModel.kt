package com.pharmatrade.feature.pharmacyorder.presentation.ordermode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderMode
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacySupplier
import com.pharmatrade.feature.pharmacyorder.domain.usecase.CreateOrderUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetSuppliersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrderModeUiState(
    val selectedMode: OrderMode? = null,
    val notes: String = "",
    val suppliers: List<PharmacySupplier> = emptyList(),
    val isLoadingSuppliers: Boolean = false,
    val suppliersError: String? = null,
    val selectedSupplier: PharmacySupplier? = null,
    val isCreating: Boolean = false,
    val createError: String? = null,
    val createdOrderId: String? = null
) {
    val canConfirm: Boolean
        get() = when (selectedMode) {
            OrderMode.BEST_DISCOUNT -> true
            OrderMode.SPECIFIC_SUPPLIER -> selectedSupplier != null
            null -> false
        }
}

class OrderModeViewModel(
    private val getSuppliersUseCase: GetSuppliersUseCase,
    private val createOrderUseCase: CreateOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderModeUiState())
    val uiState: StateFlow<OrderModeUiState> = _uiState.asStateFlow()

    fun onModeSelected(mode: OrderMode) {
        update { copy(selectedMode = mode, createError = null) }
        if (mode == OrderMode.SPECIFIC_SUPPLIER && _uiState.value.suppliers.isEmpty()) {
            loadSuppliers()
        }
    }

    fun onSupplierSelected(supplier: PharmacySupplier) = update { copy(selectedSupplier = supplier, createError = null) }

    fun onNotesChange(notes: String) = update { copy(notes = notes) }

    private fun loadSuppliers() {
        viewModelScope.launch {
            update { copy(isLoadingSuppliers = true, suppliersError = null) }
            when (val result = getSuppliersUseCase()) {
                is Result.Success -> update { copy(isLoadingSuppliers = false, suppliers = result.data) }
                is Result.Error -> update { copy(isLoadingSuppliers = false, suppliersError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun confirm() {
        val mode = _uiState.value.selectedMode ?: return
        viewModelScope.launch {
            update { copy(isCreating = true, createError = null) }
            when (val result = createOrderUseCase(mode, _uiState.value.notes)) {
                is Result.Success -> update { copy(isCreating = false, createdOrderId = result.data.id) }
                is Result.Error -> update { copy(isCreating = false, createError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    private fun update(block: OrderModeUiState.() -> OrderModeUiState) {
        _uiState.value = _uiState.value.block()
    }
}
