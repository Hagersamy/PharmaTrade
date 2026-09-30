package com.pharmatrade.feature.catalog.presentation.drugs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.catalog.domain.usecase.GetAllSellersUseCase
import com.pharmatrade.feature.catalog.domain.usecase.GetSellerListingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SellerDrugsUiState(
    val seller: Seller? = null,
    val listings: List<SellerListing> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val addedToCartListing: SellerListing? = null
)

class SellerDrugsViewModel(
    private val getListingsUseCase: GetSellerListingsUseCase,
    private val getAllSellersUseCase: GetAllSellersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerDrugsUiState())
    val uiState: StateFlow<SellerDrugsUiState> = _uiState.asStateFlow()

    fun loadSeller(sellerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val sellersResult = getAllSellersUseCase()
            val seller = (sellersResult as? Result.Success)?.data?.find { it.id == sellerId }
            val listingsResult = getListingsUseCase(sellerId)
            val listings = (listingsResult as? Result.Success)?.data ?: emptyList()
            val error = (listingsResult as? Result.Error)?.message?.let { LanguageManager.strings.friendlyError(it) }

            _uiState.value = _uiState.value.copy(
                isLoading = false, seller = seller, listings = listings, error = error
            )
        }
    }

    fun onAddedToCart(listing: SellerListing) {
        _uiState.value = _uiState.value.copy(addedToCartListing = listing)
    }

    fun onAddedToCartHandled() {
        _uiState.value = _uiState.value.copy(addedToCartListing = null)
    }
}
