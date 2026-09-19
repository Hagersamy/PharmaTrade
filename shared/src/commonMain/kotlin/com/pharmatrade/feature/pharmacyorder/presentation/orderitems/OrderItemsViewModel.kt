package com.pharmatrade.feature.pharmacyorder.presentation.orderitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.drugs.domain.usecase.GetDrugsUseCase
import com.pharmatrade.feature.pharmacyorder.domain.model.DraftOrderItem
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacySupplier
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierInventoryItem
import com.pharmatrade.feature.pharmacyorder.domain.model.UploadItemsResult
import com.pharmatrade.feature.pharmacyorder.domain.usecase.AddOrderItemUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetOrderDetailUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetSupplierInventoryUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.RemoveOrderItemUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.UploadOrderItemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// A drug picked (either from the generic catalog search or a specific supplier's inventory)
// awaiting a quantity before it's added to the order.
data class PendingAddItem(
    val drugId: String,
    val displayName: String,
    val priceInfo: String? = null
)

data class OrderItemsUiState(
    // Generic catalog search — best_discount mode
    val searchQuery: String = "",
    val searchResults: List<Drug> = emptyList(),
    val isSearching: Boolean = false,
    // Specific-supplier inventory browse — specific_supplier mode
    val supplier: PharmacySupplier? = null,
    val supplierInventory: List<SupplierInventoryItem> = emptyList(),
    val supplierInventoryFilter: String = "",
    val isLoadingSupplierInventory: Boolean = false,
    val supplierInventoryError: String? = null,
    // Shared add-item flow
    val pendingItem: PendingAddItem? = null,
    val isAddingItem: Boolean = false,
    val addItemError: String? = null,
    // Inventory row ids currently being quick-added (row-level spinner, no dialog). Keyed by
    // the row's own id rather than drugId so a backend response with a missing/duplicated
    // drug_id can't make every row light up together.
    val quickAddingItemIds: Set<String> = emptySet(),
    val items: List<DraftOrderItem> = emptyList(),
    val isLoadingItems: Boolean = false,
    val itemsError: String? = null,
    val isUploading: Boolean = false,
    val uploadResult: UploadItemsResult? = null,
    val uploadError: String? = null
)

class OrderItemsViewModel(
    private val orderId: String,
    private val supplierId: String?,
    private val getDrugsUseCase: GetDrugsUseCase,
    private val getSupplierInventoryUseCase: GetSupplierInventoryUseCase,
    private val addOrderItemUseCase: AddOrderItemUseCase,
    private val removeOrderItemUseCase: RemoveOrderItemUseCase,
    private val uploadOrderItemsUseCase: UploadOrderItemsUseCase,
    private val getOrderDetailUseCase: GetOrderDetailUseCase
) : ViewModel() {

    val isSpecificSupplier: Boolean = !supplierId.isNullOrBlank()

    private val _uiState = MutableStateFlow(OrderItemsUiState())
    val uiState: StateFlow<OrderItemsUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadItems()
        if (isSpecificSupplier) {
            loadSupplierInventory()
        } else {
            viewModelScope.launch {
                searchQueryFlow.debounce(300).distinctUntilChanged().collect { query ->
                    if (query.isBlank()) {
                        update { copy(searchResults = emptyList(), isSearching = false) }
                        return@collect
                    }
                    update { copy(isSearching = true) }
                    when (val result = getDrugsUseCase(search = query, perPage = 20)) {
                        is Result.Success -> update { copy(isSearching = false, searchResults = result.data) }
                        is Result.Error -> update { copy(isSearching = false, searchResults = emptyList()) }
                        is Result.Loading -> Unit
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        update { copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    fun onSupplierInventoryFilterChange(text: String) = update { copy(supplierInventoryFilter = text) }

    private fun loadSupplierInventory() {
        val sId = supplierId ?: return
        viewModelScope.launch {
            update { copy(isLoadingSupplierInventory = true, supplierInventoryError = null) }
            when (val result = getSupplierInventoryUseCase(sId)) {
                is Result.Success -> update {
                    copy(
                        isLoadingSupplierInventory = false,
                        supplier = result.data.supplier,
                        supplierInventory = result.data.items
                    )
                }
                is Result.Error -> update {
                    copy(isLoadingSupplierInventory = false, supplierInventoryError = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun loadItems() {
        viewModelScope.launch {
            update { copy(isLoadingItems = true, itemsError = null) }
            when (val result = getOrderDetailUseCase(orderId)) {
                is Result.Success -> update { copy(isLoadingItems = false, items = result.data.items) }
                is Result.Error -> update {
                    copy(isLoadingItems = false, itemsError = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onDrugSelected(drug: Drug) = update {
        copy(pendingItem = PendingAddItem(drugId = drug.id, displayName = drug.name), addItemError = null)
    }

    fun onSupplierItemSelected(item: SupplierInventoryItem) = update {
        copy(
            pendingItem = PendingAddItem(
                drugId = item.drugId,
                displayName = item.drugName,
                priceInfo = "EGP ${"%.2f".format(item.effectivePrice)}/unit · ${item.quantityAvailable} in stock"
            ),
            addItemError = null
        )
    }

    fun dismissQuantityDialog() = update { copy(pendingItem = null) }

    // Quick-add: tapping the "+" button adds qty=1 straight away, no dialog.
    fun quickAddSupplierItem(item: SupplierInventoryItem) {
        viewModelScope.launch {
            update { copy(quickAddingItemIds = quickAddingItemIds + item.id, addItemError = null) }
            when (val result = addOrderItemUseCase(orderId, item.drugId, 1)) {
                is Result.Success -> update {
                    copy(quickAddingItemIds = quickAddingItemIds - item.id, items = items + result.data)
                }
                is Result.Error -> update {
                    copy(
                        quickAddingItemIds = quickAddingItemIds - item.id,
                        addItemError = LanguageManager.strings.friendlyError(result.message)
                    )
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun confirmAddItem(quantity: Int) {
        val item = _uiState.value.pendingItem ?: return
        viewModelScope.launch {
            update { copy(isAddingItem = true, addItemError = null) }
            when (val result = addOrderItemUseCase(orderId, item.drugId, quantity)) {
                is Result.Success -> update {
                    copy(
                        isAddingItem = false,
                        pendingItem = null,
                        items = items + result.data,
                        searchQuery = "",
                        searchResults = emptyList()
                    )
                }
                is Result.Error -> update {
                    copy(isAddingItem = false, addItemError = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun removeItem(itemId: String) {
        val previous = _uiState.value.items
        update { copy(items = items.filterNot { it.id == itemId }) }
        viewModelScope.launch {
            when (val result = removeOrderItemUseCase(orderId, itemId)) {
                is Result.Error -> update {
                    copy(items = previous, itemsError = LanguageManager.strings.friendlyError(result.message))
                }
                else -> Unit
            }
        }
    }

    fun uploadFile(fileUri: String, fileName: String) {
        viewModelScope.launch {
            update { copy(isUploading = true, uploadError = null, uploadResult = null) }
            when (val result = uploadOrderItemsUseCase(orderId, fileUri, fileName)) {
                is Result.Success -> {
                    update { copy(isUploading = false, uploadResult = result.data) }
                    loadItems()
                }
                is Result.Error -> update {
                    copy(isUploading = false, uploadError = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun dismissUploadResult() = update { copy(uploadResult = null) }
    fun dismissUploadError() = update { copy(uploadError = null) }

    private fun update(block: OrderItemsUiState.() -> OrderItemsUiState) {
        _uiState.value = _uiState.value.block()
    }
}
