package com.pharmatrade.feature.pharmacyorder.presentation.allocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.pharmacyorder.domain.PharmacyCartBus
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderDetail
import com.pharmatrade.feature.pharmacyorder.domain.usecase.AllocateOrderUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.CancelOrderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AllocationUiState(
    val isLoading: Boolean = true,
    val order: OrderDetail? = null,
    val error: String? = null,
    val isCancelling: Boolean = false,
    val cancelled: Boolean = false
)

class AllocationViewModel(
    private val orderId: String,
    private val supplierId: String?,
    private val allocateOrderUseCase: AllocateOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllocationUiState())
    val uiState: StateFlow<AllocationUiState> = _uiState.asStateFlow()

    init {
        allocate()
    }

    fun allocate() {
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            when (val result = allocateOrderUseCase(orderId, supplierId)) {
                is Result.Success -> {
                    update { copy(isLoading = false, order = result.data) }
                    // allocate() is what actually submits/finalizes the order server-side — by
                    // this point Home's "keep adding items to this draft" order id is dead
                    // whether the buyer confirms, cancels, or just backs out without deciding.
                    // The order itself stays pending on the backend either way (only "Cancel
                    // order" actually cancels it) — this only tells Home to stop reusing the
                    // order id so the next "+" tap starts a fresh draft instead of erroring.
                    supplierId?.let { PharmacyCartBus.notifyResolved(it) }
                }
                is Result.Error -> update {
                    copy(isLoading = false, error = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun cancelOrder() {
        if (_uiState.value.isCancelling) return
        viewModelScope.launch {
            update { copy(isCancelling = true) }
            when (val result = cancelOrderUseCase(orderId)) {
                is Result.Success -> update { copy(isCancelling = false, cancelled = true) }
                is Result.Error -> update {
                    copy(isCancelling = false, error = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun update(block: AllocationUiState.() -> AllocationUiState) {
        _uiState.value = _uiState.value.block()
    }
}
