package com.pharmatrade.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val phone: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateTo: LoginNavigation? = null
)

sealed class LoginNavigation {
    object SellerDashboard : LoginNavigation()
    object BuyerCatalog : LoginNavigation()
    object AdminDashboard : LoginNavigation()
    object PendingApproval : LoginNavigation()
}

class LoginViewModel(private val loginUseCase: LoginUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onPhoneChange(phone: String) {
        _uiState.value = _uiState.value.copy(phone = phone, error = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = loginUseCase(_uiState.value.phone, _uiState.value.password)
            when (result) {
                is Result.Success -> {
                    val user = result.data
                    val nav = when {
                        user.userType == UserType.ADMIN -> LoginNavigation.AdminDashboard
                        user.userType == UserType.SELLER -> LoginNavigation.SellerDashboard
                        else -> LoginNavigation.BuyerCatalog
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, navigateTo = nav)
                }
                is Result.Error -> {
                    val msg = result.message ?: ""
                    if (msg.contains("pending", ignoreCase = true)) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            navigateTo = LoginNavigation.PendingApproval
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = msg)
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(navigateTo = null)
    }
}
