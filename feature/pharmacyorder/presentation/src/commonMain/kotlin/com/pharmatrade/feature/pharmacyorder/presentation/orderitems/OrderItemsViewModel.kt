package com.pharmatrade.feature.pharmacyorder.presentation.orderitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.error.userMessage
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.drugs.domain.usecase.GetDrugsUseCase
import com.pharmatrade.feature.pharmacyorder.domain.PharmacyCartBus
import com.pharmatrade.feature.pharmacyorder.domain.SupplierOrderLine
import com.pharmatrade.feature.pharmacyorder.domain.SupplierPageOrder
import com.pharmatrade.feature.pharmacyorder.domain.model.DraftOrderItem
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacySupplier
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem
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
    private val getOrderDetailUseCase: GetOrderDetailUseCase,
    // Lines the order already holds when opened from the Home cart — drafts' lines can't be
    // re-read from the backend, so the cart hands over what it tracked.
    initialItems: List<DraftOrderItem> = emptyList()
) : ViewModel() {

    val isSpecificSupplier: Boolean = !supplierId.isNullOrBlank()

    private val _uiState = MutableStateFlow(OrderItemsUiState(items = initialItems))

    // The order is handed to the Home cart once, on whichever comes first: "Review" or leaving
    // the page (back) — so the cart always reflects what was done here.
    private var publishedToCart = false
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
                    copy(isLoadingSupplierInventory = false, supplierInventoryError = LanguageManager.strings.userMessage(result))
                }
                is Result.Loading -> Unit
            }
        }
    }

    // NOTE: GET pharmacy/orders/{id} does not return an `items` array for draft orders, so the
    // lines can't be re-read from the backend — `items` here is tracked from the add/remove
    // responses instead. An empty result must never overwrite what we already hold.
    private fun loadItems() {
        viewModelScope.launch {
            update { copy(isLoadingItems = true, itemsError = null) }
            when (val result = getOrderDetailUseCase(orderId)) {
                is Result.Success -> update {
                    copy(isLoadingItems = false, items = result.data.items.ifEmpty { items })
                }
                is Result.Error -> update {
                    copy(isLoadingItems = false, itemsError = LanguageManager.strings.userMessage(result))
                }
                is Result.Loading -> Unit
            }
        }
    }

    // Replaces a returned line in place (same id) instead of appending a second copy of it.
    private fun List<DraftOrderItem>.upsert(item: DraftOrderItem): List<DraftOrderItem> =
        if (any { it.id == item.id }) map { if (it.id == item.id) item else it } else this + item

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

    // "+" on the card: one more than what's in the order, capped at stock.
    fun quickAddSupplierItem(item: SupplierInventoryItem) {
        val current = quantityInOrder(item.drugId)
        if (current >= item.quantityAvailable) return
        setSupplierItemQuantity(item, current + 1)
    }

    // "−" on the card: one less; reaching 0 drops the drug from the order.
    fun quickRemoveSupplierItem(item: SupplierInventoryItem) {
        val current = quantityInOrder(item.drugId)
        if (current <= 0) return
        setSupplierItemQuantity(item, current - 1)
    }

    // Sets the drug's quantity to an absolute value. The backend has no update-quantity call
    // (only an add and a remove-whole-line) and how repeat adds of the same drug combine isn't
    // something we can observe (drafts' lines aren't readable via GET), so rather than send a
    // "+1" and guess the result — which double-counted — this removes the drug's existing
    // line(s) and re-adds the exact target as one fresh line. Same approach as the Home cart.
    private fun setSupplierItemQuantity(item: SupplierInventoryItem, target: Int) {
        if (item.id in _uiState.value.quickAddingItemIds) return
        val oldLines = _uiState.value.items.filter { it.drugId == item.drugId }
        viewModelScope.launch {
            update { copy(quickAddingItemIds = quickAddingItemIds + item.id, addItemError = null) }
            var failure: String? = null
            val removedIds = mutableSetOf<String>()
            for (line in oldLines) {
                when (val result = removeOrderItemUseCase(orderId, line.id)) {
                    is Result.Error -> { failure = result.message; break }
                    else -> removedIds += line.id
                }
            }
            var added: DraftOrderItem? = null
            if (failure == null && target > 0) {
                when (val result = addOrderItemUseCase(orderId, item.drugId, target)) {
                    is Result.Success -> added = result.data
                    is Result.Error -> failure = result.message
                    is Result.Loading -> Unit
                }
            }
            update {
                val kept = items.filterNot { it.id in removedIds }
                copy(
                    items = added?.let { kept.upsert(it) } ?: kept,
                    quickAddingItemIds = quickAddingItemIds - item.id,
                    addItemError = failure?.let { LanguageManager.strings.friendlyError(it) }
                )
            }
        }
    }

    private fun quantityInOrder(drugId: String): Int =
        _uiState.value.items.filter { it.drugId == drugId }.sumOf { it.quantity }

    fun confirmAddItem(quantity: Int) {
        val item = _uiState.value.pendingItem ?: return
        viewModelScope.launch {
            update { copy(isAddingItem = true, addItemError = null) }
            when (val result = addOrderItemUseCase(orderId, item.drugId, quantity)) {
                is Result.Success -> {
                    update {
                        copy(
                            isAddingItem = false,
                            pendingItem = null,
                            items = items.upsert(result.data),
                            searchQuery = "",
                            searchResults = emptyList()
                        )
                    }
                }
                is Result.Error -> update {
                    copy(isAddingItem = false, addItemError = LanguageManager.strings.userMessage(result))
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
                    copy(items = previous, itemsError = LanguageManager.strings.userMessage(result))
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
                    copy(isUploading = false, uploadError = LanguageManager.strings.userMessage(result))
                }
                is Result.Loading -> Unit
            }
        }
    }

    // Hands this supplier's order (as built here) to the Home cart. Only for a specific supplier —
    // the cart groups orders by supplier, so a best-discount order has no place there.
    fun publishToCart() {
        val sId = supplierId?.takeIf { it.isNotBlank() } ?: return
        if (publishedToCart) return
        publishedToCart = true
        val state = _uiState.value
        val inventoryByDrugId = state.supplierInventory.associateBy { it.drugId }
        val lines = state.items.map { line ->
            val catalogItem = inventoryByDrugId[line.drugId]?.let { inv ->
                SupplierCatalogItem(
                    id = inv.id,
                    supplierId = sId,
                    supplierName = state.supplier?.name.orEmpty(),
                    drugId = inv.drugId,
                    drugName = inv.drugName,
                    dosageForm = inv.dosageForm,
                    strength = inv.strength,
                    quantityAvailable = inv.quantityAvailable,
                    unitPrice = inv.unitPrice,
                    publicPrice = inv.unitPrice,
                    pharmacistPrice = inv.unitPrice,
                    discountPct = inv.discountPct,
                    effectivePrice = inv.effectivePrice,
                    inventoryId = inv.id
                )
            }
            SupplierOrderLine(item = line, catalogItem = catalogItem)
        }
        PharmacyCartBus.publishSupplierPageOrder(SupplierPageOrder(orderId, sId, lines))
    }

    override fun onCleared() {
        publishToCart()
    }

    fun dismissUploadResult() = update { copy(uploadResult = null) }
    fun dismissUploadError() = update { copy(uploadError = null) }

    private fun update(block: OrderItemsUiState.() -> OrderItemsUiState) {
        _uiState.value = _uiState.value.block()
    }
}
