package com.pharmatrade.feature.marketplace.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.userMessage
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.marketplace.domain.model.CollectionItemInput
import com.pharmatrade.feature.marketplace.domain.model.MarketCollection
import com.pharmatrade.feature.marketplace.domain.model.MarketInventoryItem
import com.pharmatrade.feature.marketplace.domain.usecase.DeactivateCollectionUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.GetBrowseCollectionUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.GetMarketInventoryUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.GetMyCollectionUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.PlaceMarketOrderUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.PublishCollectionUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.SetCollectionItemsUseCase
import com.pharmatrade.feature.marketplace.domain.usecase.SetFeaturedItemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Seller-only: picking which stock goes into a draft collection. Quantities are kept as the raw
// text typed so a half-edited field isn't reformatted under the cursor.
data class ItemsEditorState(
    val inventory: List<MarketInventoryItem> = emptyList(),
    val quantities: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

// Seller-only: picking up to MAX_FEATURED_ITEMS of the collection's items to show on its card.
data class FeaturedPickerState(
    val selected: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val error: String? = null
)

data class MarketCollectionUiState(
    val collection: MarketCollection? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isActing: Boolean = false,
    val message: String? = null,
    val editor: ItemsEditorState? = null,
    val featuredPicker: FeaturedPickerState? = null,
    // Set after the buyer orders this collection — the screen moves on to the order.
    val placedOrderId: String? = null
)

class MarketCollectionViewModel(
    private val collectionId: String,
    // true: the current pharmacy's own collection (seller view); false: browsing someone else's.
    val mine: Boolean,
    private val getMyCollectionUseCase: GetMyCollectionUseCase,
    private val getBrowseCollectionUseCase: GetBrowseCollectionUseCase,
    private val placeMarketOrderUseCase: PlaceMarketOrderUseCase,
    private val setCollectionItemsUseCase: SetCollectionItemsUseCase,
    private val setFeaturedItemsUseCase: SetFeaturedItemsUseCase,
    private val publishCollectionUseCase: PublishCollectionUseCase,
    private val deactivateCollectionUseCase: DeactivateCollectionUseCase,
    private val getMarketInventoryUseCase: GetMarketInventoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketCollectionUiState())
    val uiState: StateFlow<MarketCollectionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.collection == null, error = null) }
            val result = if (mine) getMyCollectionUseCase(collectionId) else getBrowseCollectionUseCase(collectionId)
            when (result) {
                is Result.Success -> _uiState.update { it.copy(collection = result.data, isLoading = false) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.toMessage()) }
                is Result.Loading -> Unit
            }
        }
    }

    // ── Buyer ──

    fun placeOrder() {
        if (_uiState.value.isActing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActing = true) }
            when (val result = placeMarketOrderUseCase(collectionId)) {
                is Result.Success -> _uiState.update { it.copy(isActing = false, placedOrderId = result.data.id) }
                is Result.Error -> {
                    _uiState.update { it.copy(isActing = false, message = result.toMessage()) }
                    // Most likely another pharmacy ordered it first — show its current state.
                    load()
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onOrderOpened() = _uiState.update { it.copy(placedOrderId = null) }

    // ── Seller ──

    fun publish() = runCollectionAction(LanguageManager.strings.mkPublishedToast) { publishCollectionUseCase(collectionId) }

    fun deactivate() = runCollectionAction(LanguageManager.strings.mkUnpublishedToast) { deactivateCollectionUseCase(collectionId) }

    fun openEditor() {
        val current = _uiState.value.collection ?: return
        _uiState.update {
            it.copy(editor = ItemsEditorState(quantities = current.items.associate { item -> item.inventoryId to item.quantity.toString() }))
        }
        viewModelScope.launch {
            when (val result = getMarketInventoryUseCase()) {
                is Result.Success -> updateEditor { copy(inventory = result.data, isLoading = false) }
                is Result.Error -> updateEditor { copy(isLoading = false, error = result.toMessage()) }
                is Result.Loading -> Unit
            }
        }
    }

    fun dismissEditor() = _uiState.update { if (it.editor?.isSaving == true) it else it.copy(editor = null) }

    fun onEditorQuantityChange(inventoryId: String, text: String) {
        val max = _uiState.value.editor?.inventory?.firstOrNull { it.id == inventoryId }?.quantityAvailable ?: Int.MAX_VALUE
        val digits = text.filter { it.isDigit() }.take(6)
        val clamped = digits.toIntOrNull()?.let { if (it > max) max.toString() else digits } ?: digits
        updateEditor { copy(quantities = quantities + (inventoryId to clamped), error = null) }
    }

    fun saveEditor() {
        val editor = _uiState.value.editor ?: return
        if (editor.isSaving) return
        val items = editor.quantities.mapNotNull { (id, qty) ->
            qty.toIntOrNull()?.takeIf { it > 0 }?.let { CollectionItemInput(inventoryId = id, quantity = it) }
        }
        viewModelScope.launch {
            updateEditor { copy(isSaving = true, error = null) }
            when (val result = setCollectionItemsUseCase(collectionId, items)) {
                // Replacing the items clears the featured picks server-side, so ask for them again.
                is Result.Success -> _uiState.update {
                    it.copy(
                        collection = result.data,
                        editor = null,
                        featuredPicker = FeaturedPickerState().takeIf { result.data.items.isNotEmpty() },
                        message = LanguageManager.strings.mkItemsSavedToast
                    )
                }
                is Result.Error -> updateEditor { copy(isSaving = false, error = result.toMessage()) }
                is Result.Loading -> Unit
            }
        }
    }

    fun openFeaturedPicker() {
        val current = _uiState.value.collection ?: return
        _uiState.update { it.copy(featuredPicker = FeaturedPickerState(selected = current.featuredItemIds)) }
    }

    // Tapping a third drug is ignored until one of the two picked is unticked.
    fun onFeaturedToggle(itemId: String) = updateFeaturedPicker {
        when {
            itemId in selected -> copy(selected = selected - itemId, error = null)
            selected.size < MarketCollection.MAX_FEATURED_ITEMS -> copy(selected = selected + itemId, error = null)
            else -> this
        }
    }

    fun dismissFeaturedPicker() = _uiState.update { if (it.featuredPicker?.isSaving == true) it else it.copy(featuredPicker = null) }

    fun saveFeatured() {
        val picker = _uiState.value.featuredPicker ?: return
        if (picker.isSaving) return
        // Keep the collection's item order rather than the order they were tapped in.
        val ids = _uiState.value.collection?.items?.map { it.id }?.filter { it in picker.selected }.orEmpty()
        viewModelScope.launch {
            updateFeaturedPicker { copy(isSaving = true, error = null) }
            when (val result = setFeaturedItemsUseCase(collectionId, ids)) {
                is Result.Success -> _uiState.update {
                    it.copy(collection = result.data, featuredPicker = null, message = LanguageManager.strings.mkFeaturedSavedToast)
                }
                is Result.Error -> updateFeaturedPicker { copy(isSaving = false, error = result.toMessage()) }
                is Result.Loading -> Unit
            }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    // ── Helpers ──

    private fun runCollectionAction(successMessage: String, call: suspend () -> Result<MarketCollection>) {
        if (_uiState.value.isActing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActing = true) }
            when (val result = call()) {
                is Result.Success -> _uiState.update {
                    // Some endpoints return the collection without its items — keep the ones we have.
                    val previous = it.collection
                    val updated = result.data.let { c ->
                        if (c.items.isEmpty() && previous != null) {
                            c.copy(items = previous.items, itemsCount = previous.itemsCount, featuredItems = previous.featuredItems)
                        } else {
                            c
                        }
                    }
                    it.copy(isActing = false, collection = updated, message = successMessage)
                }
                is Result.Error -> _uiState.update { it.copy(isActing = false, message = result.toMessage()) }
                is Result.Loading -> Unit
            }
        }
    }

    private fun updateEditor(block: ItemsEditorState.() -> ItemsEditorState) =
        _uiState.update { state -> state.copy(editor = state.editor?.block()) }

    private fun updateFeaturedPicker(block: FeaturedPickerState.() -> FeaturedPickerState) =
        _uiState.update { state -> state.copy(featuredPicker = state.featuredPicker?.block()) }

    private fun Result.Error.toMessage() = LanguageManager.strings.userMessage(this)
}
