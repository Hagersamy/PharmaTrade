package com.pharmatrade.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.catalog.domain.usecase.GetAllSellersUseCase
import com.pharmatrade.feature.catalog.domain.usecase.SearchListingsUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class DrugSortOrder { BEST_PRICE, A_Z }

data class BuyerSearchUiState(
    val query: String = "",
    val filterSellers: Boolean = true,
    val filterDrugs: Boolean = true,
    val drugSortOrder: DrugSortOrder = DrugSortOrder.BEST_PRICE,
    val allSellers: List<Seller> = emptyList(),
    val filteredSellers: List<Seller> = emptyList(),
    val filteredListings: List<SellerListing> = emptyList(),
    val isLoadingSellers: Boolean = false,
    val isSearchingDrugs: Boolean = false
)

@OptIn(FlowPreview::class)
class BuyerSearchViewModel(
    private val getAllSellersUseCase: GetAllSellersUseCase,
    private val searchListingsUseCase: SearchListingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuyerSearchUiState())
    val uiState: StateFlow<BuyerSearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        loadSellers()
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query -> performSearch(query) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        queryFlow.value = query
    }

    fun toggleFilterSellers() {
        val current = _uiState.value
        // Keep at least one filter active
        if (current.filterSellers && !current.filterDrugs) return
        _uiState.value = current.copy(filterSellers = !current.filterSellers)
        performSearch(current.query)
    }

    fun toggleFilterDrugs() {
        val current = _uiState.value
        if (current.filterDrugs && !current.filterSellers) return
        _uiState.value = current.copy(filterDrugs = !current.filterDrugs)
        performSearch(current.query)
    }

    fun onDrugSortChange(sort: DrugSortOrder) {
        val current = _uiState.value
        _uiState.value = current.copy(
            drugSortOrder = sort,
            filteredListings = current.filteredListings.sortedWith(sort)
        )
    }

    private fun loadSellers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSellers = true)
            when (val result = getAllSellersUseCase()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingSellers = false,
                    allSellers = result.data
                )
                else -> _uiState.value = _uiState.value.copy(isLoadingSellers = false)
            }
        }
    }

    private fun performSearch(query: String) {
        val state = _uiState.value
        if (query.isBlank()) {
            _uiState.value = state.copy(filteredSellers = emptyList(), filteredListings = emptyList())
            return
        }
        // Client-side seller filter
        if (state.filterSellers) {
            val matchedSellers = state.allSellers.filter { seller ->
                seller.businessName.contains(query, ignoreCase = true) ||
                        seller.ownerName.contains(query, ignoreCase = true) ||
                        seller.location.contains(query, ignoreCase = true)
            }
            _uiState.value = _uiState.value.copy(filteredSellers = matchedSellers)
        } else {
            _uiState.value = _uiState.value.copy(filteredSellers = emptyList())
        }
        // Drug listing search via use case
        if (state.filterDrugs) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isSearchingDrugs = true)
                when (val result = searchListingsUseCase(query)) {
                    is Result.Success -> _uiState.value = _uiState.value.copy(
                        isSearchingDrugs = false,
                        filteredListings = result.data.sortedWith(_uiState.value.drugSortOrder)
                    )
                    else -> _uiState.value = _uiState.value.copy(
                        isSearchingDrugs = false,
                        filteredListings = emptyList()
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(filteredListings = emptyList())
        }
    }
}

private fun List<SellerListing>.sortedWith(sort: DrugSortOrder) = when (sort) {
    DrugSortOrder.BEST_PRICE -> sortedBy { it.finalPrice }
    DrugSortOrder.A_Z -> sortedBy { it.drug.name.lowercase() }
}
