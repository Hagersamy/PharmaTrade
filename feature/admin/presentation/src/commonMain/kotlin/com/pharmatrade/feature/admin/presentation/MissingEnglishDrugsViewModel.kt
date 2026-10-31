package com.pharmatrade.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.domain.model.AdminForbiddenException
import com.pharmatrade.feature.admin.domain.model.DrugEnglishNameUpdate
import com.pharmatrade.feature.admin.domain.model.DrugField
import com.pharmatrade.feature.admin.domain.model.DrugFieldError
import com.pharmatrade.feature.admin.domain.model.DrugNotFoundException
import com.pharmatrade.feature.admin.domain.model.DrugValidationException
import com.pharmatrade.feature.admin.domain.model.MissingEnglishDrug
import com.pharmatrade.feature.admin.domain.usecase.GetDrugsMissingEnglishUseCase
import com.pharmatrade.feature.admin.domain.usecase.SaveDrugEnglishNameUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MissingEnglishDrugsUiState(
    val drugs: List<MissingEnglishDrug> = emptyList(),
    // From the API's "total" — the badge on the admin home card.
    val total: Int = 0,
    val page: Int = 0,
    val hasNextPage: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false,

    // Edit sheet — open while editing != null.
    val editing: MissingEnglishDrug? = null,
    val form: DrugEnglishNameUpdate = DrugEnglishNameUpdate(englishName = ""),
    val showMoreDetails: Boolean = false,
    val fieldErrors: Map<DrugField, DrugFieldError> = emptyMap(),
    val isSaving: Boolean = false,
    val saveError: String? = null,

    val toastMessage: String? = null
)

// Shared by the admin dashboard (for the badge count) and the list screen, so a save on the list
// is reflected on the dashboard without a refetch. Nothing loads in init: the owner is created for
// every role, and only admins may call admin/*.
class MissingEnglishDrugsViewModel(
    private val getDrugsMissingEnglish: GetDrugsMissingEnglishUseCase,
    private val saveDrugEnglishName: SaveDrugEnglishNameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MissingEnglishDrugsUiState())
    val uiState: StateFlow<MissingEnglishDrugsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    // pull = started by pull-to-refresh: keep the list on screen and show the refresh indicator.
    fun refresh(pull: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = !pull && it.drugs.isEmpty(), isRefreshing = pull, error = null)
            }
            when (val result = getDrugsMissingEnglish(page = 1)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        drugs = result.data.items,
                        total = result.data.total,
                        page = result.data.currentPage,
                        hasNextPage = result.data.hasNextPage,
                        isLoading = false,
                        isRefreshing = false,
                        loaded = true
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = result.toMessage(), loaded = true)
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasNextPage || state.isLoadingMore || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = getDrugsMissingEnglish(page = state.page + 1)) {
                is Result.Success -> _uiState.update { current ->
                    val known = current.drugs.map { it.id }.toSet()
                    current.copy(
                        drugs = current.drugs + result.data.items.filter { it.id !in known },
                        total = result.data.total,
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

    // ── Edit sheet ──────────────────────────────────────────────────────────

    fun openEditor(drug: MissingEnglishDrug) {
        _uiState.update {
            it.copy(
                editing = drug,
                form = DrugEnglishNameUpdate(englishName = ""),
                showMoreDetails = false,
                fieldErrors = emptyMap(),
                saveError = null
            )
        }
    }

    fun dismissEditor() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(editing = null) }
    }

    fun onFormChange(form: DrugEnglishNameUpdate) {
        _uiState.update { state ->
            // Clear the error on whichever input was just edited.
            val old = state.form
            val edited = buildSet {
                if (form.englishName != old.englishName) add(DrugField.ENGLISH_NAME)
                if (form.scientificName != old.scientificName) add(DrugField.SCIENTIFIC_NAME)
                if (form.manufacturer != old.manufacturer) add(DrugField.MANUFACTURER)
                if (form.dosageForm != old.dosageForm) add(DrugField.DOSAGE_FORM)
                if (form.strength != old.strength) add(DrugField.STRENGTH)
                if (form.barcode != old.barcode) add(DrugField.BARCODE)
            }
            state.copy(form = form, fieldErrors = state.fieldErrors - edited, saveError = null)
        }
    }

    fun toggleMoreDetails() = _uiState.update { it.copy(showMoreDetails = !it.showMoreDetails) }

    fun save() {
        val state = _uiState.value
        val drug = state.editing ?: return
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null, fieldErrors = emptyMap()) }
            val result = saveDrugEnglishName(drug.id, state.form)
            when {
                result is Result.Success -> _uiState.update { current ->
                    // It no longer appears in the missing-english list.
                    current.copy(
                        drugs = current.drugs.filterNot { it.id == drug.id },
                        total = (current.total - 1).coerceAtLeast(0),
                        editing = null,
                        isSaving = false,
                        toastMessage = LanguageManager.strings.adDrugNameSaved
                    )
                }
                result is Result.Error -> when (val cause = result.exception) {
                    is DrugValidationException -> {
                        val errors = cause.fieldErrors
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                fieldErrors = errors,
                                // Reveal an optional input the backend rejected (e.g. a taken barcode).
                                showMoreDetails = it.showMoreDetails || errors.keys.any { f -> f != DrugField.ENGLISH_NAME }
                            )
                        }
                    }
                    is DrugNotFoundException -> {
                        _uiState.update {
                            it.copy(isSaving = false, editing = null, toastMessage = LanguageManager.strings.adDrugDeleted)
                        }
                        refresh()
                    }
                    else -> _uiState.update { it.copy(isSaving = false, saveError = result.toMessage()) }
                }
            }
        }
    }

    fun clearToast() = _uiState.update { it.copy(toastMessage = null) }

    private fun Result.Error.toMessage(): String =
        if (exception is AdminForbiddenException) LanguageManager.strings.adAdminsOnly
        else LanguageManager.strings.friendlyError(message)
}
