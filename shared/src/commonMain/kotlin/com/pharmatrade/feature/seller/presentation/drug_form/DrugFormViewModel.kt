package com.pharmatrade.feature.seller.presentation.drug_form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.drugs.domain.usecase.CreateInventoryItemUseCase
import com.pharmatrade.feature.drugs.domain.usecase.CreateSupplierDrugUseCase
import com.pharmatrade.feature.drugs.domain.usecase.GetDrugsUseCase
import com.pharmatrade.feature.seller.data.fake.FakeSellerData
import com.pharmatrade.feature.seller.domain.usecase.UpdateListingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// Some catalog rows have junk/test data where the name is just a bare number (e.g. "10", "140")
// instead of an actual drug name — hide those from the picker rather than showing them to sellers.
private val NUMERIC_NAME_REGEX = Regex("^\\d+(\\.\\d+)?$")
private fun List<Drug>.withoutNumericNames(): List<Drug> = filterNot { NUMERIC_NAME_REGEX.matches(it.name.trim()) }

data class DrugFormUiState(
    val editingListingId: String? = null,
    val selectedDrugId: String = "",
    val priceInput: String = "",
    val discountInput: String = "0",
    val quantityInput: String = "",
    val unitInput: String = "Box",
    val expiryInput: String = "",
    val availableDrugs: List<Drug> = emptyList(),
    val drugSearchQuery: String = "",
    val isSearchingDrugs: Boolean = false,
    val unitOptions: List<String> = listOf("Box", "Strip", "Bottle", "Vial", "Sachet"),
    val isLoadingDrugs: Boolean = false,
    val drugsError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    // "Can't find your drug?" fallback — registers one new drug via POST supplier/drugs
    val showAddDrugDialog: Boolean = false,
    val newDrugName: String = "",
    val newDrugTradeName: String = "",
    val newDrugScientificName: String = "",
    val newDrugManufacturer: String = "",
    val newDrugDosageForm: String = "",
    val newDrugStrength: String = "",
    val newDrugBarcode: String = "",
    val isCreatingDrug: Boolean = false,
    val addDrugError: String? = null
)

class DrugFormViewModel(
    private val createInventoryItemUseCase: CreateInventoryItemUseCase,
    private val updateListingUseCase: UpdateListingUseCase,
    private val getDrugsUseCase: GetDrugsUseCase,
    private val createSupplierDrugUseCase: CreateSupplierDrugUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrugFormUiState())
    val uiState: StateFlow<DrugFormUiState> = _uiState.asStateFlow()

    private val drugSearchQueryFlow = MutableStateFlow("")

    init {
        loadDrugs()
        // Debounced live search against GET /drugs?search= as the seller types — clearing the
        // box falls back to the initial unfiltered browse list from loadDrugs() rather than an
        // empty dropdown.
        viewModelScope.launch {
            drugSearchQueryFlow.debounce(300).distinctUntilChanged().collect { query ->
                if (query.isBlank()) {
                    loadDrugs()
                    return@collect
                }
                _uiState.value = _uiState.value.copy(isSearchingDrugs = true, drugsError = null)
                when (val result = getDrugsUseCase(search = query)) {
                    is Result.Success -> {
                        val filtered = result.data.withoutNumericNames()
                        println("$TAG: dropdown (search=\"$query\") showing ${filtered.size}/${result.data.size} drugs: " +
                            filtered.map { "${it.id} -> \"${it.name}\"" })
                        _uiState.value = _uiState.value.copy(
                            isSearchingDrugs = false,
                            availableDrugs = filtered
                        )
                    }
                    is Result.Error -> _uiState.value = _uiState.value.copy(
                        isSearchingDrugs = false,
                        drugsError = LanguageManager.strings.friendlyError(result.message)
                    )
                    else -> _uiState.value = _uiState.value.copy(isSearchingDrugs = false)
                }
            }
        }
    }

    fun onDrugSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(drugSearchQuery = query)
        drugSearchQueryFlow.value = query
    }

    fun loadDrugs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDrugs = true, drugsError = null)
            when (val result = getDrugsUseCase()) {
                is Result.Success -> {
                    val filtered = result.data.withoutNumericNames()
                    println("$TAG: dropdown (unfiltered) showing ${filtered.size}/${result.data.size} drugs: " +
                        filtered.map { "${it.id} -> \"${it.name}\"" })
                    _uiState.value = _uiState.value.copy(
                        isLoadingDrugs = false,
                        availableDrugs = filtered
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingDrugs = false,
                    drugsError = LanguageManager.strings.friendlyError(result.message)
                )
                else -> _uiState.value = _uiState.value.copy(isLoadingDrugs = false)
            }
        }
    }

    fun loadForEdit(listingId: String) {
        val listing = FakeSellerData.getListingById(listingId) ?: return
        _uiState.value = _uiState.value.copy(
            editingListingId = listingId,
            selectedDrugId = listing.drug.id,
            priceInput = listing.pricePerUnit.toString(),
            discountInput = listing.discountPercentage.toString(),
            quantityInput = listing.quantityAvailable.toString(),
            unitInput = listing.unit,
            expiryInput = listing.expiryDate
        )
    }

    fun onDrugSelected(drugId: String) {
        val drug = _uiState.value.availableDrugs.find { it.id == drugId }
        _uiState.value = _uiState.value.copy(
            selectedDrugId = drugId,
            drugSearchQuery = drug?.name ?: _uiState.value.drugSearchQuery,
            error = null
        )
    }
    fun onPriceChange(v: String) { _uiState.value = _uiState.value.copy(priceInput = v, error = null) }
    fun onDiscountChange(v: String) { _uiState.value = _uiState.value.copy(discountInput = v, error = null) }
    fun onQuantityChange(v: String) { _uiState.value = _uiState.value.copy(quantityInput = v, error = null) }
    fun onUnitChange(v: String) { _uiState.value = _uiState.value.copy(unitInput = v) }
    fun onExpiryChange(v: String) { _uiState.value = _uiState.value.copy(expiryInput = v, error = null) }

    val selectedDrug: Drug? get() = _uiState.value.availableDrugs.find { it.id == _uiState.value.selectedDrugId }

    fun openAddDrugDialog() {
        val query = _uiState.value.drugSearchQuery.trim()
        _uiState.value = _uiState.value.copy(
            showAddDrugDialog = true,
            addDrugError = null,
            newDrugName = query,
            newDrugTradeName = query
        )
    }

    fun dismissAddDrugDialog() {
        _uiState.value = _uiState.value.copy(showAddDrugDialog = false, addDrugError = null)
    }

    fun onNewDrugNameChange(v: String) { _uiState.value = _uiState.value.copy(newDrugName = v, addDrugError = null) }
    fun onNewDrugTradeNameChange(v: String) { _uiState.value = _uiState.value.copy(newDrugTradeName = v, addDrugError = null) }
    fun onNewDrugScientificNameChange(v: String) { _uiState.value = _uiState.value.copy(newDrugScientificName = v) }
    fun onNewDrugManufacturerChange(v: String) { _uiState.value = _uiState.value.copy(newDrugManufacturer = v) }
    fun onNewDrugDosageFormChange(v: String) { _uiState.value = _uiState.value.copy(newDrugDosageForm = v) }
    fun onNewDrugStrengthChange(v: String) { _uiState.value = _uiState.value.copy(newDrugStrength = v) }
    fun onNewDrugBarcodeChange(v: String) { _uiState.value = _uiState.value.copy(newDrugBarcode = v) }

    fun createNewDrug() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingDrug = true, addDrugError = null)
            val result = createSupplierDrugUseCase(
                name = state.newDrugName,
                tradeName = state.newDrugTradeName,
                scientificName = state.newDrugScientificName,
                manufacturer = state.newDrugManufacturer,
                dosageForm = state.newDrugDosageForm,
                strength = state.newDrugStrength,
                barcode = state.newDrugBarcode.takeIf { it.isNotBlank() }
            )
            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isCreatingDrug = false,
                    showAddDrugDialog = false,
                    availableDrugs = listOf(result.data) + _uiState.value.availableDrugs,
                    selectedDrugId = result.data.id,
                    newDrugName = "",
                    newDrugTradeName = "",
                    newDrugScientificName = "",
                    newDrugManufacturer = "",
                    newDrugDosageForm = "",
                    newDrugStrength = "",
                    newDrugBarcode = ""
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isCreatingDrug = false,
                    addDrugError = LanguageManager.strings.friendlyError(result.message)
                )
                else -> _uiState.value = _uiState.value.copy(isCreatingDrug = false)
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val price = state.priceInput.toDoubleOrNull() ?: run {
            _uiState.value = state.copy(error = LanguageManager.strings.errorInvalidPrice)
            return
        }
        val discount = state.discountInput.toDoubleOrNull() ?: run {
            _uiState.value = state.copy(error = LanguageManager.strings.errorInvalidDiscount)
            return
        }
        val quantity = state.quantityInput.toIntOrNull() ?: run {
            _uiState.value = state.copy(error = LanguageManager.strings.errorInvalidQuantity)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = if (state.editingListingId != null) {
                updateListingUseCase(state.editingListingId, price, discount, quantity, state.expiryInput)
            } else {
                val drug = selectedDrug ?: run {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = LanguageManager.strings.errorSelectDrug)
                    return@launch
                }
                createInventoryItemUseCase(drug.name, quantity, price, discount)
            }
            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, error = LanguageManager.strings.friendlyError(result.message)
                )
                else -> Unit
            }
        }
    }

    companion object {
        private const val TAG = "DrugFormViewModel"
    }
}
