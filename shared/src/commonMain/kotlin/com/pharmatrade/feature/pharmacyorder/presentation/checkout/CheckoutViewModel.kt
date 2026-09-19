package com.pharmatrade.feature.pharmacyorder.presentation.checkout

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

data class CheckoutOrderState(
    val orderId: String,
    val supplierId: String,
    val isLoading: Boolean = true,
    val order: OrderDetail? = null,
    val error: String? = null,
    val isCancelling: Boolean = false
)

data class CheckoutUiState(val orders: List<CheckoutOrderState> = emptyList()) {
    val grandTotal: Double get() = orders.mapNotNull { it.order?.totalValue }.sum()
}

// Checks out every supplier order the buyer had in their cart at once: allocates (submits) each
// one independently on init, shows them together on one summary, and lets the buyer drop any
// single order out of the batch (cancels it) before leaving. Same non-destructive-back principle
// as the single-order Allocation flow — leaving this screen never cancels anything on its own;
// only the explicit remove action per order does.
class CheckoutViewModel(
    orderSupplierPairs: List<Pair<String, String>>,
    private val allocateOrderUseCase: AllocateOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CheckoutUiState(orders = orderSupplierPairs.map { (orderId, supplierId) -> CheckoutOrderState(orderId, supplierId) })
    )
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        _uiState.value.orders.forEach { allocate(it.orderId, it.supplierId) }
    }

    private fun allocate(orderId: String, supplierId: String) {
        viewModelScope.launch {
            when (val result = allocateOrderUseCase(orderId, supplierId)) {
                is Result.Success -> {
                    updateOrder(orderId) { copy(isLoading = false, order = result.data, error = null) }
                    // Submitted server-side now regardless of what the buyer does next on this
                    // screen — tell Home to stop reusing this supplier's draft order id.
                    PharmacyCartBus.notifyResolved(supplierId)
                }
                is Result.Error -> updateOrder(orderId) {
                    copy(isLoading = false, error = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun removeOrder(orderId: String) {
        val target = _uiState.value.orders.find { it.orderId == orderId } ?: return
        if (target.isCancelling) return
        viewModelScope.launch {
            updateOrder(orderId) { copy(isCancelling = true) }
            when (val result = cancelOrderUseCase(orderId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(orders = _uiState.value.orders.filterNot { it.orderId == orderId })
                }
                is Result.Error -> updateOrder(orderId) {
                    copy(isCancelling = false, error = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun updateOrder(orderId: String, block: CheckoutOrderState.() -> CheckoutOrderState) {
        _uiState.value = _uiState.value.copy(
            orders = _uiState.value.orders.map { if (it.orderId == orderId) it.block() else it }
        )
    }
}
