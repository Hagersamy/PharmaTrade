package com.pharmatrade.feature.marketplace.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.userMessage
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.marketplace.domain.model.MarketOrder
import com.pharmatrade.feature.marketplace.domain.model.MarketOrderStatus
import com.pharmatrade.feature.marketplace.domain.model.SellerOrderAction
import com.pharmatrade.feature.marketplace.domain.usecase.CancelMarketPurchaseUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.GetMarketOrderUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.SellerMarketOrderActionUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.SetCollectorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketOrderUiState(
    val order: MarketOrder? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isActing: Boolean = false,
    val collectorName: String = "",
    val collectorPhone: String = "",
    val collectorError: String? = null,
    val message: String? = null
)

class MarketOrderViewModel(
    private val orderId: String,
    // true: the current pharmacy sold this collection; false: it bought it.
    val asSeller: Boolean,
    private val getMarketOrderUseCase: GetMarketOrderUseCase,
    private val setCollectorUseCase: SetCollectorUseCase,
    private val cancelMarketPurchaseUseCase: CancelMarketPurchaseUseCase,
    private val sellerMarketOrderActionUseCase: SellerMarketOrderActionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketOrderUiState())
    val uiState: StateFlow<MarketOrderUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.order == null, error = null) }
            when (val result = getMarketOrderUseCase(orderId, asSeller)) {
                is Result.Success -> _uiState.update { it.copy(order = result.data, isLoading = false) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.toMessage()) }
                is Result.Loading -> Unit
            }
        }
    }

    // ── Buyer ──

    fun onCollectorNameChange(value: String) = _uiState.update { it.copy(collectorName = value, collectorError = null) }
    fun onCollectorPhoneChange(value: String) =
        _uiState.update { it.copy(collectorPhone = value.filter { c -> c.isDigit() || c == '+' }, collectorError = null) }

    fun submitCollector() {
        val state = _uiState.value
        if (state.isActing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActing = true, collectorError = null) }
            when (val result = setCollectorUseCase(orderId, state.collectorName, state.collectorPhone)) {
                is Result.Success -> applyUpdated(result.data)
                is Result.Error -> _uiState.update { it.copy(isActing = false, collectorError = result.toMessage()) }
                is Result.Loading -> Unit
            }
        }
    }

    fun cancelAsBuyer() {
        if (_uiState.value.isActing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActing = true) }
            when (val result = cancelMarketPurchaseUseCase(orderId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isActing = false,
                            order = state.order?.copy(status = MarketOrderStatus.CANCELLED),
                            message = LanguageManager.strings.mkOrderUpdatedToast
                        )
                    }
                    load()
                }
                is Result.Error -> failed(result)
                is Result.Loading -> Unit
            }
        }
    }

    // ── Seller ──

    fun sellerAction(action: SellerOrderAction) {
        if (_uiState.value.isActing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActing = true) }
            when (val result = sellerMarketOrderActionUseCase(orderId, action)) {
                is Result.Success -> applyUpdated(result.data)
                is Result.Error -> failed(result)
                is Result.Loading -> Unit
            }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    // Action responses may omit relations (collection, branches) the detail had — keep those.
    private fun applyUpdated(updated: MarketOrder) {
        _uiState.update { state ->
            val previous = state.order
            val merged = if (previous == null) updated else updated.copy(
                collectionName = updated.collectionName.ifBlank { previous.collectionName },
                buyerName = updated.buyerName.ifBlank { previous.buyerName },
                sellerName = updated.sellerName.ifBlank { previous.sellerName },
                items = updated.items.ifEmpty { previous.items },
                totalAmount = if (updated.totalAmount > 0) updated.totalAmount else previous.totalAmount,
                netAmount = if (updated.totalAmount > 0) updated.netAmount else previous.netAmount,
                commissionAmount = if (updated.totalAmount > 0) updated.commissionAmount else previous.commissionAmount,
                commissionPct = if (updated.commissionPct > 0) updated.commissionPct else previous.commissionPct
            )
            state.copy(isActing = false, order = merged, message = LanguageManager.strings.mkOrderUpdatedToast)
        }
    }

    // A rejected transition usually means the other side moved the order meanwhile — reload it.
    private fun failed(error: Result.Error) {
        _uiState.update { it.copy(isActing = false, message = error.toMessage()) }
        load()
    }

    private fun Result.Error.toMessage() = LanguageManager.strings.userMessage(this)
}
