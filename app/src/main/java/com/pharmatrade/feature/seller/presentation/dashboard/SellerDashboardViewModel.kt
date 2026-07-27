package com.pharmatrade.feature.seller.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.feature.drugs.domain.InventoryRefreshBus
import com.pharmatrade.feature.drugs.domain.model.InventoryItem
import com.pharmatrade.feature.drugs.domain.usecase.GetInventoryUseCase
import com.pharmatrade.feature.drugs.domain.usecase.UpdateInventoryItemUseCase
import com.pharmatrade.feature.seller.domain.usecase.GetSellerProfileUseCase
import com.pharmatrade.feature.seller.domain.usecase.UpdateMinimumOrderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SellerDashboardUiState(
    val seller: Seller? = null,
    val inventoryItems: List<InventoryItem> = emptyList(),
    val totalItems: Int = 0,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val showMinOrderDialog: Boolean = false,
    val newMinOrderInput: String = "",
    val snackbarMessage: String? = null,
    // Editing a single uploaded inventory item via PUT supplier/inventory/{id}
    val editingItem: InventoryItem? = null,
    val editDrugNameInput: String = "",
    val editQuantityInput: String = "",
    val editPriceInput: String = "",
    val editDiscountInput: String = "",
    val isSavingEdit: Boolean = false,
    val editError: String? = null
)

class SellerDashboardViewModel(
    private val getProfileUseCase: GetSellerProfileUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val updateMinOrderUseCase: UpdateMinimumOrderUseCase,
    private val updateInventoryItemUseCase: UpdateInventoryItemUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "SellerDashboardVM"
    }

    private val _uiState = MutableStateFlow(SellerDashboardUiState())
    val uiState: StateFlow<SellerDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
        // Fires as soon as InventoryUploadViewModel's background poll detects the backend has
        // finished processing an uploaded file — refreshes Home even before the user navigates
        // back, instead of relying solely on the screen becoming visible again.
        InventoryRefreshBus.events
            .onEach {
                Log.d(TAG, "InventoryRefreshBus event received, reloading")
                loadData()
            }
            .launchIn(viewModelScope)
    }

    fun loadData() {
        val currentUser = SessionManager.user ?: return
        val sellerId = currentUser.sellerId ?: currentUser.id
        Log.d(TAG, "loadData() called, sellerId=$sellerId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // getProfileUseCase still reads from a mock seller-profile source that doesn't know
            // about real backend accounts, so its failure is expected for real logins and must
            // not block the screen. The real business-name / min-order data already live on the
            // session user from login, so we fall back to that instead of surfacing an error.
            val profileResult = getProfileUseCase(sellerId)
            val inventoryResult = getInventoryUseCase(page = 1)

            val seller = (profileResult as? Result.Success)?.data ?: fallbackSeller(currentUser)
            val inventory = (inventoryResult as? Result.Success)?.data
            val error = (inventoryResult as? Result.Error)?.message

            Log.d(
                TAG,
                "loadData() result: items=${inventory?.items?.size}, totalItems=${inventory?.totalItems}, " +
                    "currentPage=${inventory?.currentPage}, hasMorePages=${inventory?.hasMorePages}, error=$error"
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                seller = seller,
                inventoryItems = inventory?.items ?: emptyList(),
                totalItems = inventory?.totalItems ?: 0,
                currentPage = inventory?.currentPage ?: 1,
                hasMorePages = inventory?.hasMorePages ?: false,
                error = error
            )
        }
    }

    // The backend paginates (30 items/page) and a seller's inventory can span dozens of pages,
    // so Home fetches one page at a time — this loads the next page and appends it, called as
    // the user scrolls near the bottom of the list.
    fun loadMoreInventory() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMorePages) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            when (val result = getInventoryUseCase(page = state.currentPage + 1)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    inventoryItems = _uiState.value.inventoryItems + result.data.items,
                    currentPage = result.data.currentPage,
                    hasMorePages = result.data.hasMorePages
                )
                else -> _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    private fun fallbackSeller(user: User) = Seller(
        id = user.sellerId ?: user.id,
        userId = user.id,
        businessName = user.businessName.ifBlank { user.name },
        ownerName = user.name,
        phone = user.phone,
        location = user.address ?: "",
        rating = 0f,
        minimumOrderAmount = user.minOrderValue?.toDoubleOrNull() ?: 0.0,
        totalSales = 0,
        isVerified = true,
        licenseNumber = user.licenceNumber ?: ""
    )

    fun showMinOrderDialog() {
        _uiState.value = _uiState.value.copy(
            showMinOrderDialog = true,
            newMinOrderInput = _uiState.value.seller?.minimumOrderAmount?.toString() ?: ""
        )
    }

    fun onMinOrderInputChange(value: String) {
        _uiState.value = _uiState.value.copy(newMinOrderInput = value)
    }

    fun dismissMinOrderDialog() {
        _uiState.value = _uiState.value.copy(showMinOrderDialog = false)
    }

    fun updateMinimumOrder() {
        val sellerId = SessionManager.user?.sellerId ?: return
        val amount = _uiState.value.newMinOrderInput.toDoubleOrNull() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showMinOrderDialog = false)
            when (val result = updateMinOrderUseCase(sellerId, amount)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        seller = result.data,
                        snackbarMessage = "Minimum order updated to EGP ${String.format("%.0f", amount)}"
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(snackbarMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun openEditDialog(item: InventoryItem) {
        _uiState.value = _uiState.value.copy(
            editingItem = item,
            editDrugNameInput = item.drugNameRaw,
            editQuantityInput = item.quantityAvailable.toString(),
            editPriceInput = item.unitPrice.toString(),
            editDiscountInput = item.discountPct.toString(),
            editError = null
        )
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(editingItem = null, editError = null)
    }

    fun onEditDrugNameChange(v: String) { _uiState.value = _uiState.value.copy(editDrugNameInput = v, editError = null) }
    fun onEditQuantityChange(v: String) { _uiState.value = _uiState.value.copy(editQuantityInput = v, editError = null) }
    fun onEditPriceChange(v: String) { _uiState.value = _uiState.value.copy(editPriceInput = v, editError = null) }
    fun onEditDiscountChange(v: String) { _uiState.value = _uiState.value.copy(editDiscountInput = v, editError = null) }

    fun saveEdit() {
        val state = _uiState.value
        val item = state.editingItem ?: return
        val quantity = state.editQuantityInput.toIntOrNull() ?: run {
            _uiState.value = state.copy(editError = "Enter a valid quantity"); return
        }
        val price = state.editPriceInput.toDoubleOrNull() ?: run {
            _uiState.value = state.copy(editError = "Enter a valid price"); return
        }
        val discount = state.editDiscountInput.toDoubleOrNull() ?: run {
            _uiState.value = state.copy(editError = "Enter a valid discount"); return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingEdit = true, editError = null)
            val result = updateInventoryItemUseCase(
                id = item.id,
                drugNameRaw = state.editDrugNameInput,
                quantityAvailable = quantity,
                unitPrice = price,
                discountPct = discount
            )
            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSavingEdit = false,
                    editingItem = null,
                    inventoryItems = _uiState.value.inventoryItems.map { if (it.id == item.id) result.data else it },
                    snackbarMessage = "Inventory item updated"
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSavingEdit = false,
                    editError = result.message
                )
                else -> _uiState.value = _uiState.value.copy(isSavingEdit = false)
            }
        }
    }
}
