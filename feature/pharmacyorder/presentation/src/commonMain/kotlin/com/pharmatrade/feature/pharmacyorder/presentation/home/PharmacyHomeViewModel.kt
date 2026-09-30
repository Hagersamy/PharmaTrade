package com.pharmatrade.feature.pharmacyorder.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.log.debugLog
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.pharmacyorder.domain.PharmacyCartBus
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderDetail
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderMode
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem
import com.pharmatrade.feature.pharmacyorder.domain.usecase.AddOrderItemUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.CreateOrderUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetAllSuppliersDrugsUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetOrderDetailUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetPharmacyOrdersUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.RemoveOrderItemUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// One draft order-in-progress per supplier the pharmacy has added items from —
// lazily created on the first add, then reused (add-item-one-by-one) for the rest,
// since order creation doesn't take a supplier_id (only allocate does).
data class SupplierCartOrder(
    val orderId: String,
    val supplierId: String,
    val supplierName: String,
    val itemCount: Int,
    val subtotal: Double
)

data class PharmacyHomeUiState(
    val activeOrdersTotal: Int = 0,
    val isLoadingOrders: Boolean = false,
    val ordersError: String? = null,
    // All-suppliers drug catalog, browsed directly on Home. catalogItems is exactly what the
    // backend returned for catalogQuery (blank = full catalog, otherwise ?search= results),
    // page by page — no client-side filtering.
    val catalogItems: List<SupplierCatalogItem> = emptyList(),
    val catalogQuery: String = "",
    val catalogPage: Int = 0,
    val catalogHasNextPage: Boolean = false,
    val isLoadingCatalogFirstPage: Boolean = false,
    val isLoadingCatalogMore: Boolean = false,
    val catalogError: String? = null,
    // Raw search-field text; catalogQuery follows it after the debounce.
    val catalogFilter: String = "",
    // Every catalog item seen so far (any page, any search), by id. The cart looks items up here
    // rather than in catalogItems, since a search replaces catalogItems and would otherwise make
    // in-cart items from the previous list vanish from the Cart tab.
    val knownCatalogItems: Map<String, SupplierCatalogItem> = emptyMap(),
    val processingItemIds: Set<String> = emptySet(),
    val catalogActionError: String? = null,
    val pendingCatalogItem: SupplierCatalogItem? = null,
    val cartQuantities: Map<String, Int> = emptyMap(),
    // catalogItem.id -> the backend draft-order-item id backing that quantity, so an edit
    // (increment/decrement/dialog re-entry) can remove the old line and re-add the new amount
    // — the backend only exposes add (additive) and remove (whole line), no update-quantity call.
    val cartItemIds: Map<String, String> = emptyMap(),
    val supplierCarts: Map<String, SupplierCartOrder> = emptyMap()
) {
    val totalCartItems: Int get() = supplierCarts.values.sumOf { it.itemCount }
    val canLoadMoreCatalog: Boolean
        get() = catalogPage > 0 && catalogHasNextPage && !isLoadingCatalogFirstPage && !isLoadingCatalogMore
    val isSearchActive: Boolean get() = catalogQuery.isNotBlank()
}

private const val CATALOG_PAGE_SIZE = 20
private const val SEARCH_DEBOUNCE_MS = 500L
// logcat: adb logcat -s PharmaSearch
private const val SEARCH_LOG_TAG = "PharmaSearch"

class PharmacyHomeViewModel(
    private val getPharmacyOrdersUseCase: GetPharmacyOrdersUseCase,
    private val getAllSuppliersDrugsUseCase: GetAllSuppliersDrugsUseCase,
    private val getOrderDetailUseCase: GetOrderDetailUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val addOrderItemUseCase: AddOrderItemUseCase,
    private val removeOrderItemUseCase: RemoveOrderItemUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PharmacyHomeUiState())
    val uiState: StateFlow<PharmacyHomeUiState> = _uiState.asStateFlow()

    // supplierId -> draft order id. Kept separate from supplierCarts (which is derived/display
    // state and disappears once a supplier's cart empties) so the same draft order is reused if
    // the buyer re-adds items from that supplier later in this session.
    private val orderIdsBySupplier = mutableMapOf<String, String>()

    // Guards the "get-or-create draft order for this supplier" check-then-act below: two rapid
    // add-to-cart taps for different drugs from the same supplier each launch their own coroutine,
    // and without this lock both could see orderIdsBySupplier[supplierId] == null at the same time
    // and each POST a new draft order (one becomes the tracked cart, the other is orphaned as a
    // stray draft that never gets allocated or cleaned up).
    private val orderCreationMutex = Mutex()

    // The single in-flight catalog request (first page of a new query, or the next page of the
    // current one). A new query cancels it, so a slow response for an old query can never land
    // on top of the newer results. Declared above init, which starts the first load.
    private var catalogJob: Job? = null
    private var searchDebounceJob: Job? = null

    init {
        // Deliberately does NOT call loadActiveOrdersCount() here — AppNavigation's
        // entry<NavKeys.Home> lifecycle observer calls it on ON_RESUME, which fires immediately
        // (synchronous catch-up, since the Activity is already RESUMED when this VM is constructed)
        // and covers first load on its own. Calling it here too used to double that one request.
        val firstPage = startCatalog(query = "")
        viewModelScope.launch {
            // Let the first catalog page land before restoring, so draft items already on it
            // don't need their own lookup request.
            firstPage.join()
            restoreCartFromDraftOrders()
        }
        // Fires when a draft order gets confirmed or cancelled (checkout/allocation), which runs
        // on a separate back-stack entry with no direct reference back to this ViewModel — drop
        // this supplier's local cart bookkeeping (that order id is no longer usable) and refresh
        // the active-orders count so it reflects the just-confirmed order without waiting for
        // Home to be torn down and recreated.
        PharmacyCartBus.supplierOrderResolved
            .onEach { supplierId ->
                clearSupplierCart(supplierId)
                loadActiveOrdersCount()
            }
            .launchIn(viewModelScope)
    }

    // The cart here is really just whatever draft orders exist on the backend — this ViewModel's
    // own cartQuantities/cartItemIds/orderIdsBySupplier are only an in-memory projection of that,
    // rebuilt from scratch every time the app process restarts. Without this, items the buyer
    // added before the app was killed would look gone from the Cart tab even though the backend
    // still has the draft order sitting there — this reloads it so nothing disappears until the
    // buyer removes it themselves or successfully checks out.
    private suspend fun restoreCartFromDraftOrders() {
        val draftOrders = when (val result = getPharmacyOrdersUseCase(status = "draft", perPage = 100)) {
            is Result.Success -> result.data.orders
            else -> return
        }
        if (draftOrders.isEmpty()) return

        // NOTE: never treat an empty `items` here as "abandoned draft" — GET pharmacy/orders/{id}
        // does not return an `items` array for drafts at all, so every draft looks empty even
        // when it has lines. Cancelling on that basis wipes out real carts.
        val details = draftOrders.mapNotNull { summary ->
            (getOrderDetailUseCase(summary.id) as? Result.Success)?.data
        }.filter { it.items.isNotEmpty() }
        if (details.isEmpty()) return

        // A draft order has no supplier recorded on the backend until it's allocated (createOrder
        // only takes an order mode, not a supplier id — see CreateOrderRequest), so supplierOrders
        // is empty for it. Infer the supplier instead: this app only ever puts one supplier's
        // items in a given draft order (setCartQuantity reuses orderIdsBySupplier per supplier),
        // so the right supplier is whichever one has every one of this order's drugIds listed.
        fun known() = _uiState.value.knownCatalogItems.values

        fun resolveSupplierId(detail: OrderDetail): String? {
            detail.supplierOrders.firstOrNull()?.supplierId?.let { return it }
            val drugIds = detail.items.map { it.drugId }.toSet()
            val candidates = known().filter { it.drugId in drugIds }.map { it.supplierId }.toSet()
            return candidates.firstOrNull { supplierId ->
                drugIds.all { drugId -> known().any { it.supplierId == supplierId && it.drugId == drugId } }
            } ?: candidates.firstOrNull()
        }

        // Only the first catalog page is loaded by now. Rather than paging through the whole
        // catalog to find the rest, look each missing drug up by name with one ?search= request.
        val knownDrugIds = known().map { it.drugId }.toSet()
        val missing = details.flatMap { it.items }.filter { it.drugId !in knownDrugIds && it.drugName.isNotBlank() }
        val neededDrugIds = missing.map { it.drugId }.toSet()
        for (drugName in missing.map { it.drugName.trim() }.distinct()) {
            val result = getAllSuppliersDrugsUseCase(page = 1, search = drugName, perPage = CATALOG_PAGE_SIZE)
            if (result is Result.Success) {
                val found = result.data.items.filter { it.drugId in neededDrugIds }
                update { copy(knownCatalogItems = knownCatalogItems + found.associateBy { it.id }) }
            }
        }

        val catalogByKey = known().associateBy { it.supplierId to it.drugId }
        val newQuantities = mutableMapOf<String, Int>()
        val newItemIds = mutableMapOf<String, String>()
        for (detail in details) {
            val supplierId = resolveSupplierId(detail) ?: continue
            orderIdsBySupplier[supplierId] = detail.id
            for (item in detail.items) {
                val catalogItem = catalogByKey[supplierId to item.drugId] ?: continue
                newQuantities[catalogItem.id] = item.quantity
                newItemIds[catalogItem.id] = item.id
            }
        }
        if (newQuantities.isEmpty()) return
        update {
            val mergedQuantities = cartQuantities + newQuantities
            copy(
                cartQuantities = mergedQuantities,
                cartItemIds = cartItemIds + newItemIds,
                supplierCarts = recomputeSupplierCarts(mergedQuantities)
            )
        }
    }

    private fun clearSupplierCart(supplierId: String) {
        orderIdsBySupplier.remove(supplierId)
        update {
            val itemsById = knownCatalogItems
            val newQuantities = cartQuantities.filterKeys { itemsById[it]?.supplierId != supplierId }
            val newItemIds = cartItemIds.filterKeys { itemsById[it]?.supplierId != supplierId }
            copy(
                cartQuantities = newQuantities,
                cartItemIds = newItemIds,
                supplierCarts = supplierCarts - supplierId
            )
        }
    }

    fun loadActiveOrdersCount() {
        viewModelScope.launch {
            update { copy(isLoadingOrders = true, ordersError = null) }
            when (val result = getPharmacyOrdersUseCase(status = "pending_supplier_confirmation", perPage = 1)) {
                is Result.Success -> update { copy(isLoadingOrders = false, activeOrdersTotal = result.data.total) }
                is Result.Error -> update {
                    copy(isLoadingOrders = false, ordersError = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    // ── All-suppliers drug catalog ────────────────────────────────────────────

    // Called by the list's infinite scroll — fetches the next page of whatever is showing
    // (full catalog or current search), driven by the backend's has_next_page.
    fun loadNextCatalogPage() {
        val state = _uiState.value
        if (!state.canLoadMoreCatalog || catalogJob?.isActive == true) return
        val query = state.catalogQuery
        val page = state.catalogPage + 1
        debugLog(SEARCH_LOG_TAG, "scroll → load more \"$query\" page $page")
        update { copy(isLoadingCatalogMore = true, catalogError = null) }
        catalogJob = viewModelScope.launch { fetchCatalogPage(query, page) }
    }

    fun retryCatalog() {
        startCatalog(_uiState.value.catalogQuery)
    }

    // User types → wait 500ms of no typing → ONE GET pharmacy/suppliers/drugs?search=<text>&page=1.
    // Clearing the field goes straight back to the unfiltered catalog.
    fun onCatalogFilterChange(text: String) {
        update { copy(catalogFilter = text) }
        searchDebounceJob?.cancel()
        val query = text.trim()
        debugLog(SEARCH_LOG_TAG, "typed \"$text\" → waiting ${if (query.isEmpty()) 0 else SEARCH_DEBOUNCE_MS}ms")
        searchDebounceJob = viewModelScope.launch {
            if (query.isNotEmpty()) delay(SEARCH_DEBOUNCE_MS)
            val state = _uiState.value
            if (query == state.catalogQuery && (state.catalogPage > 0 || state.isLoadingCatalogFirstPage)) {
                debugLog(SEARCH_LOG_TAG, "debounce done: \"$query\" already showing/loading — no request")
                return@launch
            }
            debugLog(SEARCH_LOG_TAG, "debounce done: searching \"$query\"")
            startCatalog(query)
        }
    }

    // Drops the current list and loads page 1 for [query] (blank = full catalog).
    private fun startCatalog(query: String): Job {
        catalogJob?.cancel()
        update {
            copy(
                catalogQuery = query,
                catalogItems = emptyList(),
                catalogPage = 0,
                catalogHasNextPage = false,
                isLoadingCatalogFirstPage = true,
                isLoadingCatalogMore = false,
                catalogError = null
            )
        }
        return viewModelScope.launch { fetchCatalogPage(query, page = 1) }.also { catalogJob = it }
    }

    private suspend fun fetchCatalogPage(query: String, page: Int) {
        val result = getAllSuppliersDrugsUseCase(
            page = page,
            search = query.ifBlank { null },
            perPage = CATALOG_PAGE_SIZE
        )
        // Stale: the query changed while this request was out.
        if (_uiState.value.catalogQuery != query) {
            debugLog(SEARCH_LOG_TAG, "discarded \"$query\" page $page — query is now \"${_uiState.value.catalogQuery}\"")
            return
        }
        when (result) {
            is Result.Success -> update {
                val pageItems = result.data.items.filter { it.effectivePrice > 0 }
                result.data.items.filter { it.effectivePrice <= 0 }.forEach {
                    debugLog(SEARCH_LOG_TAG, "hidden (effective price ${it.effectivePrice} ≤ 0): id=${it.id} \"${it.drugName}\"")
                }
                val merged = (catalogItems + pageItems).distinctBy { it.id }
                val duplicates = catalogItems.size + pageItems.size - merged.size
                if (duplicates > 0) debugLog(SEARCH_LOG_TAG, "hidden $duplicates duplicate(s) (same supplier + drug id)")
                debugLog(
                    SEARCH_LOG_TAG,
                    "\"$query\" page ${result.data.currentPage}: server ${result.data.items.size} → shown ${merged.size} " +
                        "(has_next_page=${result.data.hasNextPage})"
                )
                copy(
                    isLoadingCatalogFirstPage = false,
                    isLoadingCatalogMore = false,
                    // The backend can return the same supplier+drug more than once — within a page,
                    // or again on a later page when its offset pagination shifts between requests.
                    // id (supplierId_drugId) is the LazyColumn key and the cart key, so a repeat
                    // would crash the list with "Key was already used"; keep the first occurrence.
                    catalogItems = merged,
                    catalogPage = result.data.currentPage,
                    catalogHasNextPage = result.data.hasNextPage,
                    knownCatalogItems = knownCatalogItems + pageItems.associateBy { it.id }
                )
            }
            is Result.Error -> update {
                debugLog(SEARCH_LOG_TAG, "\"$query\" page $page failed: ${result.message}")
                copy(
                    isLoadingCatalogFirstPage = false,
                    isLoadingCatalogMore = false,
                    catalogError = LanguageManager.strings.friendlyError(result.message)
                )
            }
            is Result.Loading -> Unit
        }
    }

    fun onCatalogItemTapped(item: SupplierCatalogItem) = update { copy(pendingCatalogItem = item, catalogActionError = null) }
    fun dismissCatalogDialog() = update { copy(pendingCatalogItem = null) }

    // "+" on the catalog card, or on the in-cart stepper — both just mean "one more than
    // whatever's currently in the cart for this item", capped at available stock.
    fun quickAddCatalogItem(item: SupplierCatalogItem) {
        val current = _uiState.value.cartQuantities[item.id] ?: 0
        setCartQuantity(item, current + 1)
    }

    fun incrementCartItem(item: SupplierCatalogItem) = quickAddCatalogItem(item)

    fun decrementCartItem(item: SupplierCatalogItem) {
        val current = _uiState.value.cartQuantities[item.id] ?: return
        if (current <= 0) return
        setCartQuantity(item, current - 1)
    }

    fun confirmAddCatalogItem(quantity: Int) {
        val item = _uiState.value.pendingCatalogItem ?: return
        update { copy(pendingCatalogItem = null) }
        setCartQuantity(item, quantity)
    }

    fun dismissCatalogActionError() = update { copy(catalogActionError = null) }

    // Drops every item the buyer added for this supplier — used by the "remove order" action on
    // the Cart screen. Removes each line from the backend draft order via the same per-item path
    // as the +/- stepper (setCartQuantity(item, 0)); once nothing is left, recomputeSupplierCarts
    // naturally drops the supplier's group entirely.
    fun removeSupplierOrder(supplierId: String) {
        val itemsById = _uiState.value.knownCatalogItems
        _uiState.value.cartQuantities.keys
            .filter { itemsById[it]?.supplierId == supplierId }
            .forEach { catalogItemId -> itemsById[catalogItemId]?.let { setCartQuantity(it, 0) } }
    }

    // Sets this item's cart quantity to an absolute value (not a delta), enforcing the drug's
    // available stock as a hard ceiling. Since the backend only exposes an additive "add item"
    // call and a "remove whole line" call — no update-quantity endpoint — every change here
    // removes the previously tracked line (if any) and re-adds the new amount as a fresh line,
    // so there's always exactly one order-item line per catalog item.
    private fun setCartQuantity(item: SupplierCatalogItem, requestedQuantity: Int) {
        if (requestedQuantity > item.quantityAvailable) {
            update {
                copy(catalogActionError = LanguageManager.strings.errorOnlyUnitsAvailable(item.quantityAvailable, item.drugName))
            }
            return
        }
        val targetQuantity = requestedQuantity.coerceAtLeast(0)
        if (targetQuantity == (_uiState.value.cartQuantities[item.id] ?: 0)) return

        viewModelScope.launch {
            update { copy(processingItemIds = processingItemIds + item.id, catalogActionError = null) }

            val orderId = orderIdsBySupplier[item.supplierId] ?: orderCreationMutex.withLock {
                // Re-check after acquiring the lock: another coroutine may have created and
                // stored the draft order for this supplier while this one was waiting.
                orderIdsBySupplier[item.supplierId] ?: when (val created = createOrderUseCase(OrderMode.SPECIFIC_SUPPLIER)) {
                    is Result.Success -> created.data.id.also { orderIdsBySupplier[item.supplierId] = it }
                    is Result.Error -> {
                        update {
                            copy(
                                processingItemIds = processingItemIds - item.id,
                                catalogActionError = LanguageManager.strings.friendlyError(created.message)
                            )
                        }
                        return@launch
                    }
                    is Result.Loading -> {
                        update { copy(processingItemIds = processingItemIds - item.id) }
                        return@launch
                    }
                }
            }

            _uiState.value.cartItemIds[item.id]?.let { existingItemId ->
                removeOrderItemUseCase(orderId, existingItemId)
            }

            if (targetQuantity == 0) {
                update {
                    val newQuantities = cartQuantities - item.id
                    copy(
                        processingItemIds = processingItemIds - item.id,
                        cartQuantities = newQuantities,
                        cartItemIds = cartItemIds - item.id,
                        supplierCarts = recomputeSupplierCarts(newQuantities)
                    )
                }
                return@launch
            }

            when (val added = addOrderItemUseCase(orderId, item.drugId, targetQuantity)) {
                is Result.Success -> update {
                    val newQuantities = cartQuantities + (item.id to targetQuantity)
                    copy(
                        processingItemIds = processingItemIds - item.id,
                        cartQuantities = newQuantities,
                        cartItemIds = cartItemIds + (item.id to added.data.id),
                        supplierCarts = recomputeSupplierCarts(newQuantities)
                    )
                }
                is Result.Error -> update {
                    copy(
                        processingItemIds = processingItemIds - item.id,
                        cartQuantities = cartQuantities - item.id,
                        cartItemIds = cartItemIds - item.id,
                        catalogActionError = LanguageManager.strings.friendlyError(added.message),
                        supplierCarts = recomputeSupplierCarts(cartQuantities - item.id)
                    )
                }
                is Result.Loading -> update { copy(processingItemIds = processingItemIds - item.id) }
            }
        }
    }

    private fun recomputeSupplierCarts(cartQuantities: Map<String, Int>): Map<String, SupplierCartOrder> {
        val itemsById = _uiState.value.knownCatalogItems
        return cartQuantities
            .mapNotNull { (catalogItemId, quantity) -> itemsById[catalogItemId]?.let { it to quantity } }
            .filter { (_, quantity) -> quantity > 0 }
            .groupBy { (catalogItem, _) -> catalogItem.supplierId }
            .mapValues { (supplierId, entries) ->
                SupplierCartOrder(
                    orderId = orderIdsBySupplier[supplierId].orEmpty(),
                    supplierId = supplierId,
                    supplierName = entries.first().first.supplierName,
                    itemCount = entries.sumOf { (_, quantity) -> quantity },
                    subtotal = entries.sumOf { (catalogItem, quantity) -> catalogItem.effectivePrice * quantity }
                )
            }
    }

    private fun update(block: PharmacyHomeUiState.() -> PharmacyHomeUiState) {
        _uiState.value = _uiState.value.block()
    }
}
