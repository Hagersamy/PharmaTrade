package com.pharmatrade.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.reminder.ReminderTime
import com.pharmatrade.core.common.reminder.UploadReminderStore
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.auth.domain.model.RegisterField
import com.pharmatrade.feature.auth.domain.model.RegisterFieldError
import com.pharmatrade.feature.auth.domain.model.RegisterValidationException
import com.pharmatrade.core.common.model.Zone
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
    // Daily "upload your data" reminder times — saved on-device for this account after sign-up
    val reminderTimes: List<ReminderTime> = emptyList(),
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    // Non-field problems only (network, server down, …) — field problems go in fieldErrors.
    val error: String? = null,
    // Every field that's wrong (local checks or backend 422), shown under that field.
    val fieldErrors: Map<RegisterField, RegisterFieldError> = emptyMap(),
    // Bumped on each submit that produced field errors, so the screen scrolls to the first one.
    val validationAttempt: Int = 0,
    val isSuccess: Boolean = false
) {
    fun clearing(vararg fields: RegisterField): RegisterUiState =
        if (fields.none { it in fieldErrors }) this else copy(fieldErrors = fieldErrors - fields.toSet())
}

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

    fun onNameChange(v: String) = update { copy(name = v, error = null).clearing(RegisterField.NAME) }
    fun onEmailChange(v: String) = update { copy(email = v, error = null).clearing(RegisterField.EMAIL) }
    fun onPhoneChange(v: String) = update { copy(phone = v, error = null).clearing(RegisterField.PHONE) }
    fun onPasswordChange(v: String) = update { copy(password = v, error = null).clearing(RegisterField.PASSWORD, RegisterField.CONFIRM_PASSWORD) }
    fun onConfirmPasswordChange(v: String) = update { copy(confirmPassword = v, error = null).clearing(RegisterField.CONFIRM_PASSWORD) }
    fun onBusinessNameChange(v: String) = update { copy(businessName = v, error = null).clearing(RegisterField.BUSINESS_NAME) }
    fun onUserTypeChange(type: UserType) = update { copy(userType = type, error = null).clearing(RegisterField.MIN_ORDER_VALUE, RegisterField.MIN_ORDER_QTY) }
    fun togglePasswordVisibility() = update { copy(isPasswordVisible = !isPasswordVisible) }
    fun toggleConfirmPasswordVisibility() = update { copy(isConfirmPasswordVisible = !isConfirmPasswordVisible) }
    fun onLicenceNumberChange(v: String) = update { copy(licenceNumber = v, error = null).clearing(RegisterField.LICENCE_NUMBER) }
    fun onZoneToggled(zone: Zone) = update {
        val newZones = if (selectedZones.any { it.id == zone.id }) {
            selectedZones.filterNot { it.id == zone.id }
        } else {
            selectedZones + zone
        }
        copy(selectedZones = newZones, error = null).clearing(RegisterField.ZONES)
    }
    fun onAddressChange(v: String) = update { copy(address = v, error = null).clearing(RegisterField.ADDRESS) }
    fun onLicenceFrontSelected(uri: String?) = update { copy(licenceFrontUri = uri, error = null).clearing(RegisterField.LICENCE_FRONT) }
    fun onLicenceBackSelected(uri: String?) = update { copy(licenceBackUri = uri, error = null).clearing(RegisterField.LICENCE_BACK) }
    fun onMinOrderValueChange(v: String) = update { copy(minOrderValue = v, error = null).clearing(RegisterField.MIN_ORDER_VALUE) }
    fun onMinOrderQtyChange(v: String) = update { copy(minOrderQty = v, error = null).clearing(RegisterField.MIN_ORDER_QTY) }
    fun onReminderAdded(time: ReminderTime) = update {
        if (time in reminderTimes || reminderTimes.size >= ReminderTime.MAX_PER_DAY) this
        else copy(reminderTimes = (reminderTimes + time).sorted())
    }
    fun onReminderRemoved(time: ReminderTime) = update { copy(reminderTimes = reminderTimes - time) }

    fun register() {
        val state = _uiState.value
        val localErrors = registerUseCase.validate(
            name = state.name,
            email = state.email,
            phone = state.phone,
            password = state.password,
            confirmPassword = state.confirmPassword,
            userType = state.userType,
            businessName = state.businessName,
            licenceNumber = state.licenceNumber,
            zoneIds = state.selectedZones.map { it.id.toString() },
            address = state.address,
            licenceFrontUri = state.licenceFrontUri,
            licenceBackUri = state.licenceBackUri,
            minOrderValue = state.minOrderValue,
            minOrderQty = state.minOrderQty
        )
        if (localErrors.isNotEmpty()) {
            update { copy(error = null, fieldErrors = localErrors, validationAttempt = validationAttempt + 1) }
            return
        }

        viewModelScope.launch {
            update { copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
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
                is Result.Success -> {
                    if (state.userType == UserType.SELLER) {
                        UploadReminderStore.save(state.phone, state.reminderTimes)
                    }
                    update { copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    val backendFieldErrors = (result.exception as? RegisterValidationException)?.fieldErrors
                    if (!backendFieldErrors.isNullOrEmpty()) {
                        update {
                            copy(
                                isLoading = false,
                                error = null,
                                fieldErrors = backendFieldErrors,
                                validationAttempt = validationAttempt + 1
                            )
                        }
                    } else {
                        update { copy(isLoading = false, error = LanguageManager.strings.friendlyError(result.message)) }
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun update(block: RegisterUiState.() -> RegisterUiState) {
        _uiState.value = _uiState.value.block()
    }
}
