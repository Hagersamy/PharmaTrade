package com.pharmatrade.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.usecase.ApproveRequestUseCase
import com.pharmatrade.feature.admin.domain.usecase.DeclineRequestUseCase
import com.pharmatrade.feature.admin.domain.usecase.GetRegistrationRequestsUseCase
import com.pharmatrade.feature.admin.domain.usecase.GetRegistrationStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AdminTab { PHARMACIES, SUPPLIERS }

data class AdminUiState(
    val selectedTab: AdminTab = AdminTab.PHARMACIES,
    val pendingPharmacies: List<PendingUser> = emptyList(),
    val pendingSuppliers: List<PendingUser> = emptyList(),
    val totalRegistered: Int = 0,
    val approvedThisMonth: Int = 0,
    val activeUsers: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

class AdminDashboardViewModel(
    private val getRegistrationRequestsUseCase: GetRegistrationRequestsUseCase,
    private val approveRequestUseCase: ApproveRequestUseCase,
    private val declineRequestUseCase: DeclineRequestUseCase,
    private val getRegistrationStatsUseCase: GetRegistrationStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun selectTab(tab: AdminTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun refresh() = loadAll()

    fun approveUser(user: PendingUser) {
        viewModelScope.launch {
            when (val result = approveRequestUseCase(user.id, "")) {
                is Result.Success -> {
                    removeFromList(user)
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "${user.name} approved successfully"
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    error = LanguageManager.strings.friendlyError(result.message)
                )
                is Result.Loading -> Unit
            }
        }
    }

    fun rejectUser(user: PendingUser, reason: String) {
        viewModelScope.launch {
            when (val result = declineRequestUseCase(user.id, reason)) {
                is Result.Success -> {
                    removeFromList(user)
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "${user.name} rejected"
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    error = LanguageManager.strings.friendlyError(result.message)
                )
                is Result.Loading -> Unit
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun removeFromList(user: PendingUser) {
        if (user.userType == UserType.BUYER) {
            _uiState.value = _uiState.value.copy(
                pendingPharmacies = _uiState.value.pendingPharmacies.filter { it.id != user.id }
            )
        } else {
            _uiState.value = _uiState.value.copy(
                pendingSuppliers = _uiState.value.pendingSuppliers.filter { it.id != user.id }
            )
        }
    }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val pharmaciesResult = getRegistrationRequestsUseCase("pharmacy")
            val suppliersResult = getRegistrationRequestsUseCase("supplier")
            val statsResult = getRegistrationStatsUseCase()
            val stats = if (statsResult is Result.Success) statsResult.data else null
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                pendingPharmacies = if (pharmaciesResult is Result.Success) pharmaciesResult.data else emptyList(),
                pendingSuppliers = if (suppliersResult is Result.Success) suppliersResult.data else emptyList(),
                totalRegistered = stats?.total ?: 0,
                approvedThisMonth = stats?.approved ?: 0,
                activeUsers = stats?.approved ?: 0,
                error = when {
                    pharmaciesResult is Result.Error -> LanguageManager.strings.friendlyError(pharmaciesResult.message)
                    suppliersResult is Result.Error -> LanguageManager.strings.friendlyError(suppliersResult.message)
                    else -> null
                }
            )
        }
    }
}
