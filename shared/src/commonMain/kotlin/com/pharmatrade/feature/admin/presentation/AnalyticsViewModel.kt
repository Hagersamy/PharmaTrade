package com.pharmatrade.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.model.RegistrationStats
import com.pharmatrade.feature.admin.domain.usecase.ApproveRequestUseCase
import com.pharmatrade.feature.admin.domain.usecase.DeclineRequestUseCase
import com.pharmatrade.feature.admin.domain.usecase.GetRegistrationRequestsUseCase
import com.pharmatrade.feature.admin.domain.usecase.GetRegistrationStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class EntityFilter(val value: String?, val label: String) {
    ALL(null, "All"),
    PHARMACY("pharmacy", "Pharmacies"),
    SUPPLIER("supplier", "Suppliers")
}

data class AnalyticsUiState(
    val stats: RegistrationStats = RegistrationStats(),
    val requests: List<PendingUser> = emptyList(),
    val filter: EntityFilter = EntityFilter.ALL,
    val isLoadingStats: Boolean = false,
    val isLoadingRequests: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

class AnalyticsViewModel(
    private val getStatsUseCase: GetRegistrationStatsUseCase,
    private val getRequestsUseCase: GetRegistrationRequestsUseCase,
    private val approveRequestUseCase: ApproveRequestUseCase,
    private val declineRequestUseCase: DeclineRequestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun refresh() = loadAll()

    fun setFilter(filter: EntityFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
        loadRequests(filter.value)
    }

    fun approveRequest(id: String, notes: String) {
        viewModelScope.launch {
            when (val result = approveRequestUseCase(id, notes)) {
                is Result.Success -> {
                    removeRequest(id)
                    update { copy(snackbarMessage = "Request approved successfully") }
                }
                is Result.Error -> update { copy(error = LanguageManager.strings.friendlyError(result.message)) }
                is Result.Loading -> Unit
            }
        }
    }

    fun declineRequest(id: String, reason: String) {
        viewModelScope.launch {
            when (val result = declineRequestUseCase(id, reason)) {
                is Result.Success -> {
                    removeRequest(id)
                    update { copy(snackbarMessage = "Request declined") }
                }
                is Result.Error -> update { copy(error = LanguageManager.strings.friendlyError(result.message)) }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearSnackbar() = update { copy(snackbarMessage = null) }
    fun clearError() = update { copy(error = null) }

    private fun loadAll() {
        loadStats()
        loadRequests(_uiState.value.filter.value)
    }

    private fun loadStats() {
        viewModelScope.launch {
            update { copy(isLoadingStats = true) }
            when (val result = getStatsUseCase()) {
                is Result.Success -> update { copy(isLoadingStats = false, stats = result.data) }
                is Result.Error -> update { copy(isLoadingStats = false) }
                is Result.Loading -> Unit
            }
        }
    }

    private fun loadRequests(entityType: String?) {
        viewModelScope.launch {
            update { copy(isLoadingRequests = true, error = null) }
            when (val result = getRequestsUseCase(entityType)) {
                is Result.Success -> update { copy(isLoadingRequests = false, requests = result.data) }
                is Result.Error -> update {
                    copy(isLoadingRequests = false, error = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun removeRequest(id: String) =
        update { copy(requests = requests.filter { it.id != id }) }

    private fun update(block: AnalyticsUiState.() -> AnalyticsUiState) {
        _uiState.value = _uiState.value.block()
    }
}
