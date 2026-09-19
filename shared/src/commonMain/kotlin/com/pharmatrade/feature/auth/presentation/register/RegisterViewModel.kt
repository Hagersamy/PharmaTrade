package com.pharmatrade.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.auth.domain.model.Zone
import com.pharmatrade.feature.auth.domain.repository.ZoneRepository
import com.pharmatrade.feature.auth.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val businessName: String = "",
    val userType: UserType = UserType.BUYER,
    val licenceNumber: String = "",
    val address: String = "",
    val licenceFrontUri: String? = null,
    val licenceBackUri: String? = null,
    // Zone selection state
    val zones: List<Zone> = emptyList(),
    val isLoadingZones: Boolean = false,
    val zonesError: String? = null,
    val selectedZones: List<Zone> = emptyList(),
    // Supplier (SELLER) only
    val minOrderValue: String = "",
    val minOrderQty: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
    private val zoneRepository: ZoneRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        loadZones()
    }

    fun loadZones() {
        viewModelScope.launch {
            update { copy(isLoadingZones = true, zonesError = null) }
            when (val result = zoneRepository.getZones()) {
                is Result.Success -> update { copy(isLoadingZones = false, zones = result.data) }
                is Result.Error -> update {
                    copy(isLoadingZones = false, zonesError = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onNameChange(v: String) = update { copy(name = v, error = null) }
    fun onEmailChange(v: String) = update { copy(email = v, error = null) }
    fun onPhoneChange(v: String) = update { copy(phone = v, error = null) }
    fun onPasswordChange(v: String) = update { copy(password = v, error = null) }
    fun onConfirmPasswordChange(v: String) = update { copy(confirmPassword = v, error = null) }
    fun onBusinessNameChange(v: String) = update { copy(businessName = v, error = null) }
    fun onUserTypeChange(type: UserType) = update { copy(userType = type, error = null) }
    fun togglePasswordVisibility() = update { copy(isPasswordVisible = !isPasswordVisible) }
    fun toggleConfirmPasswordVisibility() = update { copy(isConfirmPasswordVisible = !isConfirmPasswordVisible) }
    fun onLicenceNumberChange(v: String) = update { copy(licenceNumber = v, error = null) }
    fun onZoneToggled(zone: Zone) = update {
        val newZones = if (selectedZones.any { it.id == zone.id }) {
            selectedZones.filterNot { it.id == zone.id }
        } else {
            selectedZones + zone
        }
        copy(selectedZones = newZones, error = null)
    }
    fun onAddressChange(v: String) = update { copy(address = v, error = null) }
    fun onLicenceFrontSelected(uri: String?) = update { copy(licenceFrontUri = uri, error = null) }
    fun onLicenceBackSelected(uri: String?) = update { copy(licenceBackUri = uri, error = null) }
    fun onMinOrderValueChange(v: String) = update { copy(minOrderValue = v, error = null) }
    fun onMinOrderQtyChange(v: String) = update { copy(minOrderQty = v, error = null) }

    fun register() {
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            val state = _uiState.value
            val result = registerUseCase(
                name = state.name,
                email = state.email,
                phone = state.phone,
                password = state.password,
                confirmPassword = state.confirmPassword,
                userType = state.userType,
                businessName = state.businessName,
                licenceNumber = state.licenceNumber.takeIf { it.isNotBlank() },
                zoneIds = state.selectedZones.map { it.id.toString() },
                address = state.address.takeIf { it.isNotBlank() },
                licenceFrontUri  = state.licenceFrontUri,
                licenceBackUri   = state.licenceBackUri,
                minOrderValue = state.minOrderValue.takeIf { it.isNotBlank() },
                minOrderQty = state.minOrderQty.takeIf { it.isNotBlank() }
            )
            when (result) {
                is Result.Success -> update { copy(isLoading = false, isSuccess = true) }
                is Result.Error -> update {
                    copy(isLoading = false, error = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun update(block: RegisterUiState.() -> RegisterUiState) {
        _uiState.value = _uiState.value.block()
    }
}
