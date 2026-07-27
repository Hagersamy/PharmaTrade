package com.pharmatrade.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.feature.auth.domain.model.Zone
import com.pharmatrade.feature.auth.domain.repository.ZoneRepository
import com.pharmatrade.feature.pharmacyorder.domain.model.Branch
import com.pharmatrade.feature.pharmacyorder.domain.usecase.GetBranchUseCase
import com.pharmatrade.feature.profile.domain.usecase.ChangePasswordUseCase
import com.pharmatrade.feature.profile.domain.usecase.DeactivateAccountUseCase
import com.pharmatrade.feature.profile.domain.usecase.GetProfileUseCase
import com.pharmatrade.feature.profile.domain.usecase.RequestZoneUpdateUseCase
import com.pharmatrade.feature.profile.domain.usecase.UpdateBranchProfileUseCase
import com.pharmatrade.feature.profile.domain.usecase.UpdateProfileUseCase
import com.pharmatrade.feature.profile.domain.usecase.UpdateSupplierProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = SessionManager.user,
    val supplierZones: List<Zone> = emptyList(),
    val isLoadingProfile: Boolean = false,

    val branch: Branch? = null,
    val isLoadingBranch: Boolean = false,
    val branchError: String? = null,

    val showEditProfileDialog: Boolean = false,
    val editName: String = "",
    val editEmail: String = "",
    val editPhone: String = "",
    val editProfileConfirmPassword: String = "",
    val isSavingProfile: Boolean = false,
    val profileFormError: String? = null,

    val showChangePasswordDialog: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSavingPassword: Boolean = false,
    val passwordError: String? = null,

    val showEditSupplierDialog: Boolean = false,
    val editSupplierName: String = "",
    val editMinOrderValue: String = "",
    val editMinOrderQty: String = "",
    val isSavingSupplier: Boolean = false,
    val supplierError: String? = null,

    val showEditBranchDialog: Boolean = false,
    val editBranchName: String = "",
    val editBranchAddress: String = "",
    val editBranchPhone: String = "",
    val editBranchLicence: String = "",
    val isSavingBranch: Boolean = false,
    val branchFormError: String? = null,

    val showDeactivateDialog: Boolean = false,
    val isDeactivating: Boolean = false,
    val deactivateError: String? = null,
    val accountDeactivated: Boolean = false,

    val showRequestZoneDialog: Boolean = false,
    val availableZones: List<Zone> = emptyList(),
    val isLoadingZones: Boolean = false,
    val zonesError: String? = null,
    val selectedZones: List<Zone> = emptyList(),
    val zoneChangeReason: String = "",
    val isSubmittingZoneRequest: Boolean = false,
    val zoneRequestError: String? = null,

    val successMessage: String? = null
)

class ProfileViewModel(
    private val getBranchUseCase: GetBranchUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val updateSupplierProfileUseCase: UpdateSupplierProfileUseCase,
    private val updateBranchProfileUseCase: UpdateBranchProfileUseCase,
    private val deactivateAccountUseCase: DeactivateAccountUseCase,
    private val requestZoneUpdateUseCase: RequestZoneUpdateUseCase,
    private val zoneRepository: ZoneRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        if (SessionManager.user?.userType == UserType.BUYER) {
            loadBranch()
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            update { copy(isLoadingProfile = true) }
            when (val result = getProfileUseCase()) {
                is Result.Success -> update {
                    copy(
                        isLoadingProfile = false,
                        user = SessionManager.user,
                        supplierZones = result.data.supplier?.zones ?: emptyList()
                    )
                }
                is Result.Error -> update { copy(isLoadingProfile = false) }
                is Result.Loading -> Unit
            }
        }
    }

    fun loadBranch() {
        viewModelScope.launch {
            update { copy(isLoadingBranch = true, branchError = null) }
            when (val result = getBranchUseCase()) {
                is Result.Success -> update { copy(isLoadingBranch = false, branch = result.data) }
                is Result.Error -> update { copy(isLoadingBranch = false, branchError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    // --- Edit profile (name/email/phone — min-order fields have their own endpoint) ---

    fun openEditProfileDialog() {
        val u = _uiState.value.user
        update {
            copy(
                showEditProfileDialog = true,
                editName = u?.name ?: "",
                editEmail = u?.email ?: "",
                editPhone = u?.phone ?: "",
                editProfileConfirmPassword = "",
                profileFormError = null
            )
        }
    }

    fun dismissEditProfileDialog() {
        update { copy(showEditProfileDialog = false) }
    }

    fun onEditNameChange(value: String) = update { copy(editName = value) }
    fun onEditEmailChange(value: String) = update { copy(editEmail = value) }
    fun onEditPhoneChange(value: String) = update { copy(editPhone = value) }
    fun onEditProfileConfirmPasswordChange(value: String) = update { copy(editProfileConfirmPassword = value) }

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            val original = state.user
            // Only send fields the user actually changed — leave the rest out of the request.
            val name = state.editName.takeIf { it != (original?.name ?: "") }
            val email = state.editEmail.takeIf { it != (original?.email ?: "") }
            val phone = state.editPhone.takeIf { it != (original?.phone ?: "") }

            update { copy(isSavingProfile = true, profileFormError = null) }
            when (
                val result = updateProfileUseCase(
                    name,
                    email,
                    phone,
                    state.editProfileConfirmPassword
                )
            ) {
                is Result.Success -> update {
                    copy(
                        isSavingProfile = false,
                        showEditProfileDialog = false,
                        editProfileConfirmPassword = "",
                        user = SessionManager.user,
                        successMessage = "Profile updated"
                    )
                }
                is Result.Error -> update { copy(isSavingProfile = false, profileFormError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    // --- Change password ---

    fun openChangePasswordDialog() {
        update {
            copy(
                showChangePasswordDialog = true,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
                passwordError = null
            )
        }
    }

    fun dismissChangePasswordDialog() {
        update { copy(showChangePasswordDialog = false) }
    }

    fun onCurrentPasswordChange(value: String) = update { copy(currentPassword = value) }
    fun onNewPasswordChange(value: String) = update { copy(newPassword = value) }
    fun onConfirmPasswordChange(value: String) = update { copy(confirmPassword = value) }

    fun changePassword() {
        viewModelScope.launch {
            val state = _uiState.value
            val email = state.user?.email ?: ""
            update { copy(isSavingPassword = true, passwordError = null) }
            when (val result = changePasswordUseCase(email, state.currentPassword, state.newPassword, state.confirmPassword)) {
                is Result.Success -> update {
                    copy(
                        isSavingPassword = false,
                        showChangePasswordDialog = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        successMessage = "Password changed successfully"
                    )
                }
                is Result.Error -> update { copy(isSavingPassword = false, passwordError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    // --- Edit supplier info ---

    fun openEditSupplierDialog() {
        val u = _uiState.value.user
        update {
            copy(
                showEditSupplierDialog = true,
                editSupplierName = u?.businessName?.takeIf { it.isNotBlank() } ?: u?.name ?: "",
                editMinOrderValue = u?.minOrderValue ?: "",
                editMinOrderQty = u?.minOrderQty ?: "",
                supplierError = null
            )
        }
    }

    fun dismissEditSupplierDialog() {
        update { copy(showEditSupplierDialog = false) }
    }

    fun onEditSupplierNameChange(value: String) = update { copy(editSupplierName = value) }
    fun onEditMinOrderValueChange(value: String) = update { copy(editMinOrderValue = value) }
    fun onEditMinOrderQtyChange(value: String) = update { copy(editMinOrderQty = value) }

    fun saveSupplier() {
        viewModelScope.launch {
            val state = _uiState.value
            val minValue = state.editMinOrderValue.toDoubleOrNull()
            val minQty = state.editMinOrderQty.toIntOrNull()
            if (minValue == null) {
                update { copy(supplierError = "Enter a valid minimum order value") }
                return@launch
            }
            if (minQty == null) {
                update { copy(supplierError = "Enter a valid minimum order quantity") }
                return@launch
            }

            update { copy(isSavingSupplier = true, supplierError = null) }
            when (val result = updateSupplierProfileUseCase(state.editSupplierName, minValue, minQty)) {
                is Result.Success -> update {
                    copy(
                        isSavingSupplier = false,
                        showEditSupplierDialog = false,
                        user = SessionManager.user,
                        successMessage = "Supplier details updated"
                    )
                }
                is Result.Error -> update { copy(isSavingSupplier = false, supplierError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    // --- Edit branch info ---

    fun openEditBranchDialog() {
        val state = _uiState.value
        update {
            copy(
                showEditBranchDialog = true,
                editBranchName = state.branch?.name ?: state.user?.businessName ?: "",
                editBranchAddress = state.branch?.address ?: state.user?.address ?: "",
                editBranchPhone = state.user?.phone ?: "",
                editBranchLicence = state.user?.licenceNumber ?: "",
                branchFormError = null
            )
        }
    }

    fun dismissEditBranchDialog() {
        update { copy(showEditBranchDialog = false) }
    }

    fun onEditBranchNameChange(value: String) = update { copy(editBranchName = value) }
    fun onEditBranchAddressChange(value: String) = update { copy(editBranchAddress = value) }
    fun onEditBranchPhoneChange(value: String) = update { copy(editBranchPhone = value) }
    fun onEditBranchLicenceChange(value: String) = update { copy(editBranchLicence = value) }

    fun saveBranch() {
        viewModelScope.launch {
            val state = _uiState.value
            update { copy(isSavingBranch = true, branchFormError = null) }
            when (
                val result = updateBranchProfileUseCase(
                    state.editBranchName,
                    state.editBranchAddress,
                    state.editBranchPhone,
                    state.editBranchLicence
                )
            ) {
                is Result.Success -> update {
                    copy(
                        isSavingBranch = false,
                        showEditBranchDialog = false,
                        user = SessionManager.user,
                        branch = branch?.copy(name = result.data.name, address = result.data.address),
                        successMessage = "Branch details updated"
                    )
                }
                is Result.Error -> update { copy(isSavingBranch = false, branchFormError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    // --- Deactivate account ---

    fun openDeactivateDialog() {
        update { copy(showDeactivateDialog = true, deactivateError = null) }
    }

    fun dismissDeactivateDialog() {
        update { copy(showDeactivateDialog = false) }
    }

    fun deactivateAccount() {
        viewModelScope.launch {
            val phone = _uiState.value.user?.phone ?: ""
            update { copy(isDeactivating = true, deactivateError = null) }
            when (val result = deactivateAccountUseCase(phone)) {
                is Result.Success -> update {
                    copy(isDeactivating = false, showDeactivateDialog = false, accountDeactivated = true)
                }
                is Result.Error -> update { copy(isDeactivating = false, deactivateError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    // --- Request zone change (pharmacy or supplier) ---

    fun openRequestZoneDialog() {
        val state = _uiState.value
        val currentZoneIds = (state.supplierZones.ifEmpty { state.branch?.zones ?: emptyList() }).map { it.id }.toSet()
        update {
            copy(
                showRequestZoneDialog = true,
                selectedZones = availableZones.filter { it.id in currentZoneIds },
                zoneChangeReason = "",
                zoneRequestError = null
            )
        }
        if (_uiState.value.availableZones.isEmpty()) {
            loadAvailableZones()
        }
    }

    fun dismissRequestZoneDialog() {
        update { copy(showRequestZoneDialog = false) }
    }

    private fun loadAvailableZones() {
        viewModelScope.launch {
            update { copy(isLoadingZones = true, zonesError = null) }
            when (val result = zoneRepository.getZones()) {
                is Result.Success -> {
                    val currentZoneIds = (
                        _uiState.value.supplierZones.ifEmpty { _uiState.value.branch?.zones ?: emptyList() }
                        ).map { it.id }.toSet()
                    update {
                        copy(
                            isLoadingZones = false,
                            availableZones = result.data,
                            selectedZones = result.data.filter { it.id in currentZoneIds }
                        )
                    }
                }
                is Result.Error -> update { copy(isLoadingZones = false, zonesError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun onZoneToggle(zone: Zone) {
        update {
            val newSelection = if (selectedZones.any { it.id == zone.id }) {
                selectedZones.filterNot { it.id == zone.id }
            } else {
                selectedZones + zone
            }
            copy(selectedZones = newSelection, zoneRequestError = null)
        }
    }

    fun onZoneChangeReasonChange(value: String) = update { copy(zoneChangeReason = value, zoneRequestError = null) }

    fun submitZoneChangeRequest() {
        viewModelScope.launch {
            val state = _uiState.value
            update { copy(isSubmittingZoneRequest = true, zoneRequestError = null) }
            when (
                val result = requestZoneUpdateUseCase(state.selectedZones.map { it.id }, state.zoneChangeReason)
            ) {
                is Result.Success -> update {
                    copy(
                        isSubmittingZoneRequest = false,
                        showRequestZoneDialog = false,
                        successMessage = "Zone update request submitted for review"
                    )
                }
                is Result.Error -> update { copy(isSubmittingZoneRequest = false, zoneRequestError = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearSuccessMessage() {
        update { copy(successMessage = null) }
    }

    private fun update(block: ProfileUiState.() -> ProfileUiState) {
        _uiState.value = _uiState.value.block()
    }
}
