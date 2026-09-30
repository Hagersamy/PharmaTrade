package com.pharmatrade.feature.catalog.presentation.sellers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.catalog.domain.usecase.GetAllSellersUseCase
import com.pharmatrade.feature.catalog.domain.usecase.SearchListingsUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SellerBrowseUiState(
    val sellers: List<Seller> = emptyList(),
    val searchResults: List<SellerListing> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
class SellerBrowseViewModel(
    private val getAllSellersUseCase: GetAllSellersUseCase,
    private val searchListingsUseCase: SearchListingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerBrowseUiState())
    val uiState: StateFlow<SellerBrowseUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        loadSellers()
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        _uiState.value = _uiState.value.copy(isSearching = true)
                        when (val result = searchListingsUseCase(query)) {
                            is Result.Success -> _uiState.value = _uiState.value.copy(
                                isSearching = false, searchResults = result.data
                            )
                            is Result.Error -> _uiState.value = _uiState.value.copy(
                                isSearching = false
                            )
                            else -> Unit
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
                    }
                }
        }
    }

    fun loadSellers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = getAllSellersUseCase()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false, sellers = result.data
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, error = LanguageManager.strings.friendlyError(result.message)
                )
                else -> Unit
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchQuery.value = query
    }
}
