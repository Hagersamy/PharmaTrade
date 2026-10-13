package com.pharmatrade.feature.marketplace.presentation.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.userMessage
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.drugs.domain.usecase.GetDrugsUseCase
import com.pharmatrade.feature.marketplace.domain.model.MarketCollection
import com.pharmatrade.feature.marketplace.domain.model.MarketInventoryItem
import com.pharmatrade.feature.marketplace.domain.model.MarketOrder
import com.pharmatrade.feature.marketplace.domain.usecase.AddMarketInventoryItemUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.BrowseCollectionsUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.CreateCollectionUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.GetMarketInventoryUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.GetMyCollectionsUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.ImportMarketInventoryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// One ViewModel per marketplace tab. None loads in init{}: the host loads a tab the first time
// it's shown and refreshes the visible one on resume (e.g. back from an order).

private fun Result.Error.toMessage() = LanguageManager.strings.userMessage(this)

// ── Browse (buyer) ──────────────────────────────────────────────────────────

data class BrowseUiState(
    val collections: List<MarketCollection> = emptyList(),
    val page: Int = 0,
    val hasNextPage: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false
)

class MarketBrowseViewModel(private val browseCollectionsUseCase: BrowseCollectionsUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.collections.isEmpty(), error = null) }
            when (val result = browseCollectionsUseCase(page = 1)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        collections = result.data.items,
                        page = result.data.currentPage,
                        hasNextPage = result.data.hasNextPage,
                        isLoading = false,
                        loaded = true
                    )
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.toMessage(), loaded = true) }
                is Result.Loading -> Unit
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasNextPage || state.isLoadingMore || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = browseCollectionsUseCase(page = state.page + 1)) {
                is Result.Success -> _uiState.update { current ->
                    val known = current.collections.map { it.id }.toSet()
                    current.copy(
                        collections = current.collections + result.data.items.filter { it.id !in known },
                        page = result.data.currentPage,
                        hasNextPage = result.data.hasNextPage,
                        isLoadingMore = false
                    )
                }
                is Result.Error -> _uiState.update { it.copy(isLoadingMore = false, hasNextPage = false) }
                is Result.Loading -> Unit
            }
        }
    }
}

// ── Orders list (purchases for the buyer side, incoming orders for the seller side) ──

data class MarketOrdersUiState(
    val orders: List<MarketOrder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false
)

class MarketOrdersViewModel(private val loadOrders: suspend () -> Result<List<MarketOrder>>) : ViewModel() {
    private val _uiState = MutableStateFlow(MarketOrdersUiState())
    val uiState: StateFlow<MarketOrdersUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.orders.isEmpty(), error = null) }
            when (val result = loadOrders()) {
                is Result.Success -> _uiState.update {
                    it.copy(orders = result.data.sortedByDescending { o -> o.createdAt }, isLoading = false, loaded = true)
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.toMessage(), loaded = true) }
                is Result.Loading -> Unit
            }
        }
    }
}

// ── My collections (seller) ─────────────────────────────────────────────────

data class CreateCollectionForm(
    val name: String = "",
    val description: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

data class MyCollectionsUiState(
    val collections: List<MarketCollection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false,
    val createForm: CreateCollectionForm? = null,
    // Set once a new draft exists, so the screen can open it to add drugs.
    val createdCollectionId: String? = null
)

class MyCollectionsViewModel(
    private val getMyCollectionsUseCase: GetMyCollectionsUseCase,
    private val createCollectionUseCase: CreateCollectionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyCollectionsUiState())
    val uiState: StateFlow<MyCollectionsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.collections.isEmpty(), error = null) }
            when (val result = getMyCollectionsUseCase()) {
                is Result.Success -> _uiState.update {
                    it.copy(collections = result.data.sortedByDescending { c -> c.createdAt }, isLoading = false, loaded = true)
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.toMessage(), loaded = true) }
                is Result.Loading -> Unit
            }
        }
    }

    fun openCreate() = _uiState.update { it.copy(createForm = CreateCollectionForm()) }
    fun dismissCreate() = _uiState.update { if (it.createForm?.isSaving == true) it else it.copy(createForm = null) }
    fun editCreate(block: CreateCollectionForm.() -> CreateCollectionForm) =
        _uiState.update { state -> state.copy(createForm = state.createForm?.block()?.copy(error = null)) }

    fun submitCreate() {
        val form = _uiState.value.createForm ?: return
        if (form.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(createForm = form.copy(isSaving = true, error = null)) }
            when (val result = createCollectionUseCase(form.name, form.description, form.notes)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        createForm = null,
                        collections = listOf(result.data) + it.collections,
                        createdCollectionId = result.data.id
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(createForm = form.copy(isSaving = false, error = result.toMessage()))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onCreatedCollectionOpened() = _uiState.update { it.copy(createdCollectionId = null) }
}

// ── Marketplace stock (seller) ──────────────────────────────────────────────

data class AddInventoryForm(
    val drugQuery: String = "",
    val drugResults: List<Drug> = emptyList(),
    val isSearching: Boolean = false,
    val selectedDrug: Drug? = null,
    val quantity: String = "",
    val unitPrice: String = "",
    val expiryDate: String = "",
    val batchNumber: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

data class MarketInventoryUiState(
    val items: List<MarketInventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false,
    val addForm: AddInventoryForm? = null,
    val isImporting: Boolean = false,
    // One-shot toast text, cleared by onMessageShown().
    val message: String? = null
)

class MarketInventoryViewModel(
    private val getMarketInventoryUseCase: GetMarketInventoryUseCase,
    private val addMarketInventoryItemUseCase: AddMarketInventoryItemUseCase,
    private val importMarketInventoryUseCase: ImportMarketInventoryUseCase,
    private val getDrugsUseCase: GetDrugsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MarketInventoryUiState())
    val uiState: StateFlow<MarketInventoryUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.items.isEmpty(), error = null) }
            when (val result = getMarketInventoryUseCase()) {
                is Result.Success -> _uiState.update { it.copy(items = result.data, isLoading = false, loaded = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.toMessage(), loaded = true) }
                is Result.Loading -> Unit
            }
        }
    }

    fun openAdd() = _uiState.update { it.copy(addForm = AddInventoryForm()) }

    fun dismissAdd() {
        if (_uiState.value.addForm?.isSaving == true) return
        searchJob?.cancel()
        _uiState.update { it.copy(addForm = null) }
    }

    fun editAdd(block: AddInventoryForm.() -> AddInventoryForm) =
        _uiState.update { state -> state.copy(addForm = state.addForm?.block()?.copy(error = null)) }

    fun onDrugQueryChange(query: String) {
        editAdd { copy(drugQuery = query, selectedDrug = null) }
        searchJob?.cancel()
        if (query.isBlank()) {
            editAdd { copy(drugResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            editAdd { copy(isSearching = true) }
            val result = getDrugsUseCase(search = query.trim(), perPage = SEARCH_PAGE_SIZE)
            editAdd {
                copy(
                    isSearching = false,
                    drugResults = (result as? Result.Success)?.data.orEmpty()
                )
            }
        }
    }

    fun onDrugSelected(drug: Drug) = editAdd { copy(selectedDrug = drug, drugQuery = drug.name, drugResults = emptyList()) }

    fun submitAdd() {
        val form = _uiState.value.addForm ?: return
        if (form.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(addForm = form.copy(isSaving = true, error = null)) }
            val result = addMarketInventoryItemUseCase(
                drugId = form.selectedDrug?.id.orEmpty(),
                quantity = form.quantity.trim().toIntOrNull() ?: 0,
                unitPrice = form.unitPrice.trim().toDoubleOrNull() ?: 0.0,
                expiryDate = form.expiryDate,
                batchNumber = form.batchNumber
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(addForm = null, message = LanguageManager.strings.mkDrugAddedToast)
                    }
                    // The endpoint upserts per drug, so reload rather than splice the row in.
                    refresh()
                }
                is Result.Error -> _uiState.update {
                    it.copy(addForm = form.copy(isSaving = false, error = result.toMessage()))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun importFile(uri: String, fileName: String) {
        if (_uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            val message = when (val result = importMarketInventoryUseCase(uri, fileName)) {
                is Result.Success -> LanguageManager.strings.mkImportStarted
                is Result.Error -> result.toMessage()
                is Result.Loading -> null
            }
            _uiState.update { it.copy(isImporting = false, message = message) }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val SEARCH_PAGE_SIZE = 20
    }
}
