package com.pharmatrade.feature.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.cart.domain.model.Cart
import com.pharmatrade.feature.cart.domain.model.OrderValidation
import com.pharmatrade.feature.cart.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartUiState(
    val cart: Cart = Cart(),
    val validationErrors: List<OrderValidation> = emptyList(),
    val isPlacingOrder: Boolean = false,
    val orderPlacedId: String? = null,
    val error: String? = null
)

class CartViewModel(
    private val getCartUseCase: GetCartUseCase,
    private val addToCartUseCase: AddToCartUseCase,
    private val updateItemUseCase: UpdateCartItemUseCase,
    private val removeItemUseCase: RemoveFromCartUseCase,
    private val clearCartUseCase: ClearCartUseCase,
    private val validateCartUseCase: ValidateCartUseCase,
    private val placeOrderUseCase: PlaceOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    val cartItemCount: Int get() = _uiState.value.cart.getTotalItems()

    init {
        refreshCart()
    }

    private fun refreshCart() {
        val cart = getCartUseCase()
        val validations = validateCartUseCase(cart)
        _uiState.value = _uiState.value.copy(cart = cart, validationErrors = validations)
    }

    fun addToCart(listing: SellerListing, quantity: Int = 1) {
        addToCartUseCase(listing, quantity)
        refreshCart()
    }

    fun updateQuantity(listingId: String, quantity: Int) {
        updateItemUseCase(listingId, quantity)
        refreshCart()
    }

    fun removeItem(listingId: String) {
        removeItemUseCase(listingId)
        refreshCart()
    }

    fun clearCart() {
        clearCartUseCase()
        refreshCart()
    }

    fun placeOrder() {
        val cart = _uiState.value.cart
        if (cart.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Your cart is empty")
            return
        }
        val violations = validateCartUseCase(cart)
        if (violations.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(validationErrors = violations)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPlacingOrder = true, error = null)
            when (val result = placeOrderUseCase(cart)) {
                is Result.Success -> {
                    refreshCart()
                    _uiState.value = _uiState.value.copy(
                        isPlacingOrder = false,
                        orderPlacedId = result.data
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isPlacingOrder = false, error = result.message
                )
                else -> Unit
            }
        }
    }

    fun onOrderConfirmed() {
        _uiState.value = _uiState.value.copy(orderPlacedId = null)
    }

    fun onErrorDismissed() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
