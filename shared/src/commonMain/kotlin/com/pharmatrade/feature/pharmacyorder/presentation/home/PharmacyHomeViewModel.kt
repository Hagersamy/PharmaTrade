package com.pharmatrade.feature.pharmacyorder.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.pharmacyorder.domain.PharmacyCartBus
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderMode
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem
import com.pharmatrade.feature.pharmacyorder.domain.usecase.AddOrderItemUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.CreateOrderUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetAllSuppliersDrugsUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetPharmacyOrdersUseCase
import com.pharmatrade.feature.pharmacyorder.domain.usecase.RemoveOrderItemUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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
    // All-suppliers drug catalog, browsed directly on Home
    val catalogItems: List<SupplierCatalogItem> = emptyList(),
    val catalogPage: Int = 0,
    val catalogLastPage: Int = 1,
    val isLoadingCatalogFirstPage: Boolean = false,
    val isLoadingCatalogMore: Boolean = false,
    val catalogError: String? = null,
    val catalogFilter: String = "",
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
    val canLoadMoreCatalog: Boolean get() = catalogPage in 1 until catalogLastPage
}

class PharmacyHomeViewModel(
    private val getPharmacyOrdersUseCase: GetPharmacyOrdersUseCase,
    private val getAllSuppliersDrugsUseCase: GetAllSuppliersDrugsUseCase,
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

    init {
        loadActiveOrdersCount()
        loadNextCatalogPage()
        // Fires when a draft order gets confirmed or cancelled on the Allocation screen, which
        // runs on a separate back-stack entry — that order id is no longer usable, so drop this
        // supplier's local cart bookkeeping instead of trying to add more items to a dead order.
        PharmacyCartBus.supplierOrderResolved
            .onEach { supplierId -> clearSupplierCart(supplierId) }
            .launchIn(viewModelScope)
    }

    private fun clearSupplierCart(supplierId: String) {
        orderIdsBySupplier.remove(supplierId)
        update {
            val itemsById = catalogItems.associateBy { it.id }
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
                is Result.Error -> update { copy(isLoadingOrders = false, ordersError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    // ── All-suppliers drug catalog ────────────────────────────────────────────

    fun loadNextCatalogPage() {
        val state = _uiState.value
        if (state.isLoadingCatalogFirstPage || state.isLoadingCatalogMore) return
        if (state.catalogPage > 0 && state.catalogPage >= state.catalogLastPage) return
        val nextPage = state.catalogPage + 1

        viewModelScope.launch {
            update {
                if (nextPage == 1) copy(isLoadingCatalogFirstPage = true, catalogError = null) else copy(isLoadingCatalogMore = true)
            }
            when (val result = getAllSuppliersDrugsUseCase(nextPage)) {
                is Result.Success -> update {
                    copy(
                        isLoadingCatalogFirstPage = false,
                        isLoadingCatalogMore = false,
                        catalogItems = catalogItems + result.data.items,
                        catalogPage = result.data.currentPage,
                        catalogLastPage = result.data.lastPage
                    )
                }
                is Result.Error -> update { copy(isLoadingCatalogFirstPage = false, isLoadingCatalogMore = false, catalogError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun retryCatalog() {
        update { copy(catalogPage = 0, catalogItems = emptyList(), catalogLastPage = 1, catalogError = null) }
        loadNextCatalogPage()
    }

    fun onCatalogFilterChange(text: String) = update { copy(catalogFilter = text) }

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
        val itemsById = _uiState.value.catalogItems.associateBy { it.id }
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
                copy(catalogActionError = "Only ${item.quantityAvailable} unit${if (item.quantityAvailable == 1) "" else "s"} of ${item.drugName} available")
            }
            return
        }
        val targetQuantity = requestedQuantity.coerceAtLeast(0)
        if (targetQuantity == (_uiState.value.cartQuantities[item.id] ?: 0)) return

        viewModelScope.launch {
            update { copy(processingItemIds = processingItemIds + item.id, catalogActionError = null) }

            val orderId = orderIdsBySupplier[item.supplierId] ?: run {
                when (val created = createOrderUseCase(OrderMode.SPECIFIC_SUPPLIER)) {
                    is Result.Success -> created.data.id.also { orderIdsBySupplier[item.supplierId] = it }
                    is Result.Error -> {
                        update { copy(processingItemIds = processingItemIds - item.id, catalogActionError = created.message) }
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
                        catalogActionError = added.message,
                        supplierCarts = recomputeSupplierCarts(cartQuantities - item.id)
                    )
                }
                is Result.Loading -> update { copy(processingItemIds = processingItemIds - item.id) }
            }
        }
    }

    private fun recomputeSupplierCarts(cartQuantities: Map<String, Int>): Map<String, SupplierCartOrder> {
        val itemsById = _uiState.value.catalogItems.associateBy { it.id }
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
