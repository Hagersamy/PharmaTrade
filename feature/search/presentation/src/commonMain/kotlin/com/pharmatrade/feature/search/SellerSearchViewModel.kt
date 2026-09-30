package com.pharmatrade.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.feature.drugs.domain.usecase.GetDrugsUseCase
import com.pharmatrade.feature.seller.domain.usecase.GetSellerListingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class SellerSearchUiState(
    val query: String = "",
    // Browsed when the query is blank — the seller's own listings.
    val allListings: List<SellerListing> = emptyList(),
    val isLoading: Boolean = false,
    // Live backend catalog search (GET /drugs?search=) once the seller types a query — this is
    // the full drug catalog, not scoped to what the seller already lists.
    val catalogResults: List<Drug> = emptyList(),
    val isSearchingCatalog: Boolean = false,
    val catalogSearchError: String? = null
)

class SellerSearchViewModel(
    private val getListingsUseCase: GetSellerListingsUseCase,
    private val getDrugsUseCase: GetDrugsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerSearchUiState())
    val uiState: StateFlow<SellerSearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        loadListings()
        viewModelScope.launch {
            queryFlow.debounce(300).distinctUntilChanged().collect { query ->
                if (query.isBlank()) {
                    _uiState.value = _uiState.value.copy(catalogResults = emptyList(), isSearchingCatalog = false, catalogSearchError = null)
                    return@collect
                }
                _uiState.value = _uiState.value.copy(isSearchingCatalog = true, catalogSearchError = null)
                when (val result = getDrugsUseCase(search = query)) {
                    is Result.Success -> _uiState.value = _uiState.value.copy(isSearchingCatalog = false, catalogResults = result.data)
                    is Result.Error -> _uiState.value = _uiState.value.copy(
                        isSearchingCatalog = false,
                        catalogSearchError = LanguageManager.strings.friendlyError(result.message)
                    )
                    is Result.Loading -> Unit
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        queryFlow.value = query
    }

    private fun loadListings() {
        val sellerId = SessionManager.user?.sellerId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = getListingsUseCase(sellerId)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isLoading = false, allListings = result.data)
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
