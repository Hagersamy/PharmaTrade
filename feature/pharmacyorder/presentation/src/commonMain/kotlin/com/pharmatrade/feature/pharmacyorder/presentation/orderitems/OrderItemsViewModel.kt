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
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// How long +/- waits after the last tap before sending the drug's quantity (same as the Home cart).
private const val QUANTITY_SYNC_DEBOUNCE_MS = 700L

// A drug picked (either from the generic catalog search or a specific supplier's inventory)
// awaiting a quantity before it's added to the order.
data class PendingAddItem(
    val drugId: String,
    val displayName: String,
    val priceInfo: String? = null,
    // How many the order already holds — the dialog opens on this and "confirm" sets it, rather
    // than adding a fresh amount on top. 0 for a drug not in the order yet.
    val currentQuantity: Int = 0
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
    // +/- quantities (by drugId) not yet sent to the backend — shown right away, synced once the
    // buyer stops tapping. Overrides what `items` holds for that drug.
    val pendingQuantities: Map<String, Int> = emptyMap(),
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
        // No loadItems() here: the order is either brand new (nothing to read) or opened from the
        // cart with its lines handed over (initialItems) — and GET pharmacy/orders/{id} returns no
        // items for drafts anyway, so the call only cost a request.
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

    fun onSupplierItemSelected(item: SupplierInventoryItem) {
        inventoryByDrugId[item.drugId] = item
        val current = quantityInOrder(item.drugId)
        update {
            copy(
                pendingItem = PendingAddItem(
                    drugId = item.drugId,
                    displayName = item.drugName,
                    priceInfo = "EGP ${"%.2f".format(item.effectivePrice)}/unit · ${item.quantityAvailable} in stock",
                    currentQuantity = current
                ),
                addItemError = null
            )
        }
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

    // Per-drug debounce timers and locks for the +/- sync (see scheduleSync / syncSupplierItem).
    private val syncJobs = mutableMapOf<String, Job>()
    private val syncLocks = mutableMapOf<String, Mutex>()
    private val inventoryByDrugId = mutableMapOf<String, SupplierInventoryItem>()

    // Only the local quantity changes here — the backend sync is debounced per drug, so tapping
    // "+" five times quickly sends one update with the final quantity instead of five.
    private fun setSupplierItemQuantity(item: SupplierInventoryItem, target: Int) {
        inventoryByDrugId[item.drugId] = item
        update { copy(pendingQuantities = pendingQuantities + (item.drugId to target), addItemError = null) }
        scheduleSync(item.drugId)
    }

    // (Re)starts this drug's debounce timer; each new tap cancels the previous wait.
    private fun scheduleSync(drugId: String) {
        syncJobs[drugId]?.cancel()
        syncJobs[drugId] = viewModelScope.launch {
            val self = coroutineContext[Job]
            try {
                delay(QUANTITY_SYNC_DEBOUNCE_MS)
                syncSupplierItem(drugId)
            } finally {
                if (syncJobs[drugId] === self) syncJobs.remove(drugId)
            }
        }
    }

    // Pushes the drug's pending quantity to the backend. The backend has no update-quantity call
    // (only an add and a remove-whole-line) and how repeat adds of the same drug combine isn't
    // something we can observe (drafts' lines aren't readable via GET), so this removes the drug's
    // existing line(s) and re-adds the exact target as one fresh line. Same approach as the Home
    // cart. NonCancellable under a per-drug lock: a tap arriving mid-request must never abort it
    // between the remove and the add; it just schedules another sync that runs after this one.
    // Returns false if the backend rejected the change (the pending quantity is dropped, so the
    // card falls back to what the backend actually holds).
    private suspend fun syncSupplierItem(drugId: String): Boolean = withContext(NonCancellable) {
        syncLocks.getOrPut(drugId) { Mutex() }.withLock {
            val target = _uiState.value.pendingQuantities[drugId] ?: return@withLock true
            val rowId = inventoryByDrugId[drugId]?.id
            val oldLines = _uiState.value.items.filter { it.drugId == drugId }
            if (oldLines.sumOf { it.quantity } == target) {
                update { copy(pendingQuantities = pendingQuantities.dropIfStill(drugId, target)) }
                return@withLock true
            }

            update { copy(quickAddingItemIds = rowId?.let { quickAddingItemIds + it } ?: quickAddingItemIds) }
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
                when (val result = addOrderItemUseCase(orderId, drugId, target)) {
                    is Result.Success -> added = result.data
                    is Result.Error -> failure = result.message
                    is Result.Loading -> Unit
                }
                // The old line is already gone — if the new amount was rejected (e.g. over the
                // supplier's per-order limit), put the previous quantity back so the drug isn't
                // silently dropped from the order.
                val previous = oldLines.sumOf { it.quantity }
                if (failure != null && previous > 0 && removedIds.size == oldLines.size) {
                    (addOrderItemUseCase(orderId, drugId, previous) as? Result.Success)?.let { added = it.data }
                }
            }
            update {
                val kept = items.filterNot { it.id in removedIds }
                copy(
                    items = added?.let { kept.upsert(it) } ?: kept,
                    // A newer tap may have changed the target meanwhile — keep that one pending.
                    pendingQuantities = if (failure != null) pendingQuantities - drugId else pendingQuantities.dropIfStill(drugId, target),
                    quickAddingItemIds = rowId?.let { quickAddingItemIds - it } ?: quickAddingItemIds,
                    addItemError = failure?.let { LanguageManager.strings.friendlyError(it) }
                )
            }
            failure == null
        }
    }

    private fun Map<String, Int>.dropIfStill(drugId: String, target: Int) =
        if (this[drugId] == target) this - drugId else this

    // Sends any +/- changes still waiting out their debounce right away, then runs [onDone] —
    // used before "Review" and before leaving the page, so the order handed to the cart has
    // every tap. Stays on the page if a sync was rejected, so the buyer sees the error first.
    fun syncPendingThen(onDone: () -> Unit) {
        viewModelScope.launch {
            val drugIds = _uiState.value.pendingQuantities.keys.toList()
            drugIds.forEach { syncJobs.remove(it)?.cancel() }
            var allSynced = true
            for (drugId in drugIds) {
                if (!syncSupplierItem(drugId)) allSynced = false
            }
            if (allSynced) onDone()
        }
    }

    private fun quantityInOrder(drugId: String): Int =
        _uiState.value.pendingQuantities[drugId]
            ?: _uiState.value.items.filter { it.drugId == drugId }.sumOf { it.quantity }

    fun confirmAddItem(quantity: Int) {
        val item = _uiState.value.pendingItem ?: return
        // A supplier's drug: the dialog edits its quantity in the order, so set it to exactly
        // this (one sync, right away) instead of adding the amount on top of what's there.
        inventoryByDrugId[item.drugId]?.let { invItem ->
            if (quantity > invItem.quantityAvailable) {
                update { copy(addItemError = LanguageManager.strings.errorOnlyUnitsAvailable(invItem.quantityAvailable, invItem.drugName)) }
                return
            }
            syncJobs.remove(item.drugId)?.cancel()
            update { copy(pendingItem = null, pendingQuantities = pendingQuantities + (item.drugId to quantity), addItemError = null) }
            viewModelScope.launch { syncSupplierItem(item.drugId) }
            return
        }
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
                    // Same key the all-suppliers catalog uses (see getAllSuppliersDrugs), so the
                    // cart treats it as the same item.
                    id = "${sId}_${inv.drugId}",
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
