package com.pharmatrade.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.Zone
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

// Backend validation on POST admin/registration-requests/{id}/decline:
// "The decline reason field must be at least 10 characters." (422). Checked in the reject dialogs
// so the admin sees it before sending instead of getting a server error.
internal const val MIN_DECLINE_REASON_LENGTH = 10

enum class EntityFilter(val value: String?, val label: String) {
    ALL(null, "All"),
    PHARMACY("pharmacy", "Pharmacies"),
    SUPPLIER("supplier", "Suppliers"),
    ZONE_UPDATE("zone_update", "Zone updates")
}

data class AnalyticsUiState(
    val stats: RegistrationStats = RegistrationStats(),
    val requests: List<PendingUser> = emptyList(),
    // Pending entity_type=zone_update requests, always loaded (independent of [filter]) for the
    // Requests screen's "Zone updates" tab and its badge count.
    val zoneUpdates: List<PendingUser> = emptyList(),
    val isLoadingZoneUpdates: Boolean = false,
    val zoneUpdatesError: String? = null,
    // Zone id -> display name, so zone-update requests show names instead of bare ids.
    val zoneNames: Map<String, String> = emptyMap(),
    val filter: EntityFilter = EntityFilter.ALL,
    val isLoadingStats: Boolean = false,
    val isLoadingRequests: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,
    val toastMessage: String? = null
)

class AnalyticsViewModel(
    private val getStatsUseCase: GetRegistrationStatsUseCase,
    private val getRequestsUseCase: GetRegistrationRequestsUseCase,
    private val approveRequestUseCase: ApproveRequestUseCase,
    private val declineRequestUseCase: DeclineRequestUseCase,
    // Passed as a plain function (wired to ZoneRepository in :app) so :feature:admin doesn't
    // depend on the auth feature just to name zones.
    private val getZones: suspend () -> Result<List<Zone>>
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        loadZoneNames()
    }

    fun refresh() = loadAll()

    fun setFilter(filter: EntityFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
        loadRequests(filter.value)
    }

    // Removes the card right away rather than after the round trip; the toast confirms once the
    // server accepts it. On failure the card goes back where it was, with the error shown.
    fun approveRequest(id: String, notes: String) {
        val before = _uiState.value
        removeRequest(id)
        viewModelScope.launch {
            when (val result = approveRequestUseCase(id, notes)) {
                is Result.Success -> update { copy(toastMessage = LanguageManager.strings.aaRequestApprovedToast) }
                is Result.Error -> update {
                    copy(
                        requests = restore(requests, before.requests, id),
                        zoneUpdates = restore(zoneUpdates, before.zoneUpdates, id),
                        error = LanguageManager.strings.friendlyError(result.message)
                    )
                }
                is Result.Loading -> Unit
            }
        }
    }

    // Puts [id] back into [current] at its original position in [original], if it was there.
    private fun restore(current: List<PendingUser>, original: List<PendingUser>, id: String): List<PendingUser> {
        val index = original.indexOfFirst { it.id == id }
        if (index < 0 || current.any { it.id == id }) return current
        return current.toMutableList().apply { add(index.coerceAtMost(size), original[index]) }
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
    fun clearToast() = update { copy(toastMessage = null) }
    fun clearError() = update { copy(error = null) }

    fun refreshZoneUpdates() = loadZoneUpdates()

    private fun loadAll() {
        loadStats()
        loadRequests(_uiState.value.filter.value)
        loadZoneUpdates()
    }

    private fun loadZoneUpdates() {
        viewModelScope.launch {
            update { copy(isLoadingZoneUpdates = true, zoneUpdatesError = null) }
            when (val result = getRequestsUseCase(EntityFilter.ZONE_UPDATE.value)) {
                is Result.Success -> update { copy(isLoadingZoneUpdates = false, zoneUpdates = result.data) }
                is Result.Error -> update {
                    copy(isLoadingZoneUpdates = false, zoneUpdatesError = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
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

    // Best effort — on failure the cards just fall back to showing zone ids.
    private fun loadZoneNames() {
        viewModelScope.launch {
            val result = getZones()
            if (result is Result.Success) {
                update { copy(zoneNames = result.data.associate { it.id.toString() to it.displayName }) }
            }
        }
    }

    private fun removeRequest(id: String) =
        update { copy(requests = requests.filter { it.id != id }, zoneUpdates = zoneUpdates.filter { it.id != id }) }

    private fun update(block: AnalyticsUiState.() -> AnalyticsUiState) {
        _uiState.value = _uiState.value.block()
    }
}
