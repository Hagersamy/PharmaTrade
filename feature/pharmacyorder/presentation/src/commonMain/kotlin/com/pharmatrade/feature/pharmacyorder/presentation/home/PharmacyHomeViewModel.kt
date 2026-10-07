package com.pharmatrade.feature.pharmacyorder.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.userMessage
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.log.debugLog
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.pharmacyorder.domain.PharmacyCartBus
import com.pharmatrade.feature.pharmacyorder.domain.SupplierPageOrder
import com.pharmatrade.feature.pharmacyorder.domain.model.DraftOrderItem
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderMode
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem
import com.pharmatrade.feature.pharmacyorder.domain.usecase.AddOrderItemUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.CancelOrderUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.CreateOrderUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetAllSuppliersDrugsUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetPharmacyOrdersUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.RemoveOrderItemUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    // True while "Checkout all" is flushing pending quantity changes to the backend.
    val isSyncingCart: Boolean = false,
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
// How long the buyer must stop tapping +/- on an item before its quantity is sent to the backend.
private const val CART_SYNC_DEBOUNCE_MS = 700L
// logcat: adb logcat -s PharmaSearch
private const val SEARCH_LOG_TAG = "PharmaSearch"

class PharmacyHomeViewModel(
    private val getPharmacyOrdersUseCase: GetPharmacyOrdersUseCase,
    private val getAllSuppliersDrugsUseCase: GetAllSuppliersDrugsUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val addOrderItemUseCase: AddOrderItemUseCase,
    private val removeOrderItemUseCase: RemoveOrderItemUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase
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

    // catalogItem.id -> pending debounced backend sync for that item's quantity.
    private val cartSyncJobs = mutableMapOf<String, Job>()
    // catalogItem.id -> one sync at a time per item, so remove+add pairs never interleave.
    private val cartSyncLocks = mutableMapOf<String, Mutex>()
    // catalogItem.id -> quantity the backend draft order actually holds right now (cartQuantities
    // runs ahead of it while a sync is pending).
    private val syncedQuantities = mutableMapOf<String, Int>()

    // The single in-flight catalog request (first page of a new query, or the next page of the
    // current one). A new query cancels it, so a slow response for an old query can never land
    // on top of the newer results. Declared above init, which starts the first load.
    private var catalogJob: Job? = null
    private var searchDebounceJob: Job? = null
    // Reports duplicate catalog keys to Crashlytics with their cause (see the class).
    private val catalogDiagnostics = CatalogDuplicateDiagnostics()

    init {
        // Deliberately does NOT call loadActiveOrdersCount() here — AppNavigation's
        // entry<NavKeys.Home> lifecycle observer calls it on ON_RESUME, which fires immediately
        // (synchronous catch-up, since the Activity is already RESUMED when this VM is constructed)
        // and covers first load on its own. Calling it here too used to double that one request.
        startCatalog(query = "")
        // No cart restore from the backend's draft orders: GET pharmacy/orders/{id} returns no
        // `items` for drafts, so restoring cost one request per draft (25+ in practice, run back to
        // back right as the buyer opens New order — slowing the supplier list to ~5 s) and never
        // brought anything back.

        // Fires when a draft order gets confirmed or cancelled (checkout/allocation), which runs
        // on a separate back-stack entry with no direct reference back to this ViewModel — drop
        // this supplier's local cart bookkeeping (that order id is no longer usable). The
        // active-orders count is NOT refreshed here: this fires once per supplier (a 3-supplier
        // checkout = 3 events), and Home's resume refreshes it once when the buyer gets back.
        PharmacyCartBus.supplierOrderResolved
            .onEach { supplierId -> clearSupplierCart(supplierId) }
            .launchIn(viewModelScope)
        // The supplier page (New order → supplier, or a supplier opened from the Cart tab) hands
        // its order back here when the buyer leaves it.
        PharmacyCartBus.supplierPageOrder
            .onEach { applySupplierPageOrder(it) }
            .launchIn(viewModelScope)
    }

    // Folds an order built on the supplier page into the cart. Opened from this cart, it's the
    // same draft order, so the page's lines replace this supplier's cart outright. A brand-new
    // order becomes this supplier's cart — unless the cart already holds a different draft for
    // that supplier, in which case the new lines are added onto it and the extra draft cancelled,
    // keeping one draft order per supplier.
    private suspend fun applySupplierPageOrder(order: SupplierPageOrder) {
        val supplierId = order.supplierId
        fun known(drugId: String) = _uiState.value.knownCatalogItems.values
            .firstOrNull { it.supplierId == supplierId && it.drugId == drugId }

        // Prefer the catalog item Home already has (the cart is keyed by its id), then the one the
        // supplier page built from the inventory, and only then look the drug up by name.
        val fromPage = order.lines.mapNotNull { it.catalogItem }.filter { known(it.drugId) == null }
        if (fromPage.isNotEmpty()) update { copy(knownCatalogItems = knownCatalogItems + fromPage.associateBy { it.id }) }
        val missingNames = order.lines.filter { known(it.item.drugId) == null }
            .map { it.item.drugName.trim() }.filter { it.isNotBlank() }.distinct()
        for (drugName in missingNames) {
            val result = getAllSuppliersDrugsUseCase(page = 1, search = drugName, perPage = CATALOG_PAGE_SIZE)
            if (result is Result.Success) {
                val found = result.data.items.filter { it.supplierId == supplierId }
                update { copy(knownCatalogItems = knownCatalogItems + found.associateBy { it.id }) }
            }
        }

        // catalog item -> its quantity and backing line id in this order
        val incoming = order.lines
            .mapNotNull { line -> known(line.item.drugId)?.let { it to line.item } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } to lines.first().id }

        val tracked = orderIdsBySupplier[supplierId]
        if (tracked != null && tracked != order.orderId) {
            if (incoming.isEmpty()) return
            incoming.forEach { (item, line) ->
                val current = _uiState.value.cartQuantities[item.id] ?: 0
                setCartQuantity(item, (current + line.first).coerceAtMost(item.quantityAvailable))
            }
            cancelOrderUseCase(order.orderId)
            return
        }
        if (tracked == null && incoming.isEmpty()) return

        orderIdsBySupplier[supplierId] = order.orderId
        val itemsById = _uiState.value.knownCatalogItems
        // Already this cart's order → the page speaks for every one of the supplier's items.
        // Otherwise only for the ones it sent; any not-yet-synced taps on other items simply sync
        // into this order next.
        val ownedBefore = if (tracked == order.orderId) {
            (_uiState.value.cartQuantities.keys + syncedQuantities.keys + cartSyncJobs.keys)
                .filter { itemsById[it]?.supplierId == supplierId }
        } else emptyList()
        val replacedIds = (ownedBefore + incoming.keys.map { it.id }).toSet()
        replacedIds.forEach { id ->
            cartSyncJobs.remove(id)?.cancel()
            syncedQuantities.remove(id)
        }
        incoming.forEach { (item, line) -> syncedQuantities[item.id] = line.first }
        update {
            val newQuantities = cartQuantities - replacedIds + incoming.map { (item, line) -> item.id to line.first }
            copy(
                cartQuantities = newQuantities,
                cartItemIds = cartItemIds - replacedIds + incoming.map { (item, line) -> item.id to line.second },
                supplierCarts = recomputeSupplierCarts(newQuantities)
            )
        }
    }

    private fun clearSupplierCart(supplierId: String) {
        orderIdsBySupplier.remove(supplierId)
        val itemsById = _uiState.value.knownCatalogItems
        val supplierItemIds = (_uiState.value.cartQuantities.keys + syncedQuantities.keys + cartSyncJobs.keys)
            .filter { itemsById[it]?.supplierId == supplierId }
        supplierItemIds.forEach { id ->
            cartSyncJobs.remove(id)?.cancel()
            syncedQuantities.remove(id)
        }
        update {
            val newQuantities = cartQuantities.filterKeys { itemsById[it]?.supplierId != supplierId }
            val newItemIds = cartItemIds.filterKeys { itemsById[it]?.supplierId != supplierId }
            copy(
                cartQuantities = newQuantities,
                cartItemIds = newItemIds,
                supplierCarts = supplierCarts - supplierId
            )
        }
    }

    private var activeOrdersJob: Job? = null

    fun loadActiveOrdersCount() {
        // One in flight at a time — a second ask while it's running would just fetch the same number.
        if (activeOrdersJob?.isActive == true) return
        activeOrdersJob = viewModelScope.launch {
            update { copy(isLoadingOrders = true, ordersError = null) }
            when (val result = getPharmacyOrdersUseCase(status = "pending_supplier_confirmation", perPage = 1)) {
                is Result.Success -> update { copy(isLoadingOrders = false, activeOrdersTotal = result.data.total) }
                is Result.Error -> update {
                    copy(isLoadingOrders = false, ordersError = LanguageManager.strings.userMessage(result))
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
        catalogDiagnostics.reset(query)
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
        // Raw page, before any filtering or de-duplication — that's what the diagnosis needs.
        if (result is Result.Success) catalogDiagnostics.onPage(query, page, result.data)
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
                    catalogError = LanguageManager.strings.userMessage(result)
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
    // available stock as a hard ceiling. Only the local quantity changes here — the backend sync
    // is debounced per item (scheduleCartSync), so tapping "+" five times quickly sends a single
    // request with the final quantity instead of five.
    private fun setCartQuantity(item: SupplierCatalogItem, requestedQuantity: Int) {
        if (requestedQuantity > item.quantityAvailable) {
            update {
                copy(catalogActionError = LanguageManager.strings.errorOnlyUnitsAvailable(item.quantityAvailable, item.drugName))
            }
            return
        }
        val targetQuantity = requestedQuantity.coerceAtLeast(0)
        if (targetQuantity == (_uiState.value.cartQuantities[item.id] ?: 0)) return

        update {
            val newQuantities = if (targetQuantity == 0) cartQuantities - item.id else cartQuantities + (item.id to targetQuantity)
            copy(
                cartQuantities = newQuantities,
                supplierCarts = recomputeSupplierCarts(newQuantities),
                catalogActionError = null
            )
        }
        scheduleCartSync(item)
    }

    // (Re)starts this item's debounce timer; each new tap cancels the previous wait, so the sync
    // only runs once the buyer has stopped changing the quantity for CART_SYNC_DEBOUNCE_MS.
    private fun scheduleCartSync(item: SupplierCatalogItem) {
        cartSyncJobs[item.id]?.cancel()
        cartSyncJobs[item.id] = viewModelScope.launch {
            val self = coroutineContext[Job]
            try {
                delay(CART_SYNC_DEBOUNCE_MS)
                syncCartItem(item)
            } finally {
                // Only drop our own entry — a newer tap may already have replaced it.
                if (cartSyncJobs[item.id] === self) cartSyncJobs.remove(item.id)
            }
        }
    }

    // Pushes this item's current local quantity to the backend draft order. The backend only
    // exposes an additive "add item" call and a "remove whole line" call — no update-quantity
    // endpoint — so a change removes the previously tracked line (if any) and re-adds the new
    // amount as a fresh line, keeping exactly one order-item line per catalog item. Runs
    // NonCancellable under a per-item lock: a tap arriving mid-request must never abort it between
    // the remove and the add; it just schedules another sync that runs after this one.
    // Returns false if the backend rejected the change (the local quantity is rolled back).
    private suspend fun syncCartItem(item: SupplierCatalogItem): Boolean = withContext(NonCancellable) {
        cartSyncLocks.getOrPut(item.id) { Mutex() }.withLock {
            val target = _uiState.value.cartQuantities[item.id] ?: 0
            val synced = syncedQuantities[item.id] ?: 0
            if (target == synced) return@withLock true

            val orderId = draftOrderIdFor(item.supplierId) ?: run {
                rollbackCartItem(item.id, synced, message = null)
                return@withLock false
            }

            _uiState.value.cartItemIds[item.id]?.let { existingItemId ->
                val removed = removeOrderItemUseCase(orderId, existingItemId)
                if (removed is Result.Error) {
                    rollbackCartItem(item.id, synced, LanguageManager.strings.userMessage(removed))
                    return@withLock false
                }
                syncedQuantities.remove(item.id)
                update { copy(cartItemIds = cartItemIds - item.id) }
            }
            if (target == 0) return@withLock true

            when (val added = addOrderItemUseCase(orderId, item.drugId, target)) {
                is Result.Success -> {
                    syncedQuantities[item.id] = target
                    update { copy(cartItemIds = cartItemIds + (item.id to added.data.id)) }
                    true
                }
                is Result.Error -> {
                    // The old line is already gone at this point, so the backend now holds none.
                    rollbackCartItem(item.id, 0, LanguageManager.strings.userMessage(added))
                    false
                }
                is Result.Loading -> true
            }
        }
    }

    // Get-or-create the draft order for this supplier. Null (with the error shown) on failure.
    private suspend fun draftOrderIdFor(supplierId: String): String? =
        orderIdsBySupplier[supplierId] ?: orderCreationMutex.withLock {
            // Re-check after acquiring the lock: another coroutine may have created and
            // stored the draft order for this supplier while this one was waiting.
            orderIdsBySupplier[supplierId] ?: when (val created = createOrderUseCase(OrderMode.SPECIFIC_SUPPLIER)) {
                is Result.Success -> created.data.id.also { orderIdsBySupplier[supplierId] = it }
                is Result.Error -> {
                    update { copy(catalogActionError = LanguageManager.strings.userMessage(created)) }
                    null
                }
                is Result.Loading -> null
            }
        }

    // Puts the local quantity back to what the backend actually holds after a failed sync.
    private fun rollbackCartItem(catalogItemId: String, backendQuantity: Int, message: String?) {
        update {
            val newQuantities = if (backendQuantity == 0) cartQuantities - catalogItemId else cartQuantities + (catalogItemId to backendQuantity)
            copy(
                cartQuantities = newQuantities,
                supplierCarts = recomputeSupplierCarts(newQuantities),
                catalogActionError = message ?: catalogActionError
            )
        }
    }

    // "Checkout all" on the Cart tab: sends any quantity changes still waiting out their debounce
    // right away, then hands back the up-to-date (orderId, supplierId) pairs — so allocation never
    // submits a draft order that's missing the buyer's last taps. Stays on the cart if any sync
    // was rejected, so the buyer sees the error and the corrected quantity first.
    fun checkoutAll(onReady: (orders: List<Pair<String, String>>) -> Unit) {
        if (_uiState.value.isSyncingCart) return
        viewModelScope.launch {
            if (!flushPendingSyncs { true }) return@launch

            val orders = _uiState.value.supplierCarts.values
                .map { cart -> (orderIdsBySupplier[cart.supplierId] ?: cart.orderId) to cart.supplierId }
                .filter { (orderId, _) -> orderId.isNotBlank() }
            if (orders.isNotEmpty()) onReady(orders)
        }
    }

    // Supplier name tapped on the Cart tab: sends that supplier's pending +/- taps first, then
    // hands back its draft order id and current lines, so the supplier page opens on exactly
    // what the backend holds (it can't re-read a draft's lines itself).
    fun openSupplierOrder(supplierId: String, onReady: (orderId: String, lines: List<DraftOrderItem>) -> Unit) {
        if (_uiState.value.isSyncingCart) return
        viewModelScope.launch {
            if (!flushPendingSyncs { it.supplierId == supplierId }) return@launch
            val orderId = orderIdsBySupplier[supplierId] ?: return@launch
            val state = _uiState.value
            val lines = state.cartQuantities.mapNotNull { (catalogItemId, quantity) ->
                val item = state.knownCatalogItems[catalogItemId]?.takeIf { it.supplierId == supplierId }
                val lineId = state.cartItemIds[catalogItemId]
                if (item == null || lineId == null) null
                else DraftOrderItem(id = lineId, drugId = item.drugId, drugName = item.drugName, quantity = quantity)
            }
            onReady(orderId, lines)
        }
    }

    // Sends right away every matching quantity change still waiting out its debounce. False if
    // any was rejected (the corrected quantity and error are already showing).
    private suspend fun flushPendingSyncs(matches: (SupplierCatalogItem) -> Boolean): Boolean {
        update { copy(isSyncingCart = true) }
        val itemsById = _uiState.value.knownCatalogItems
        val pendingIds = cartSyncJobs.keys.filter { id -> itemsById[id]?.let(matches) == true }
        pendingIds.forEach { cartSyncJobs.remove(it)?.cancel() }
        var allSynced = true
        for (id in pendingIds) {
            val item = itemsById[id] ?: continue
            if (!syncCartItem(item)) allSynced = false
        }
        update { copy(isSyncingCart = false) }
        return allSynced
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
