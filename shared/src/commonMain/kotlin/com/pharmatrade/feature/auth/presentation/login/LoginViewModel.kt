package com.pharmatrade.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
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
    object RegistrationDeclined : LoginNavigation()
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
                    // Same substring-matching approach the "pending" branch already used — the
                    // backend doesn't return a structured status on a failed login, only this
                    // message. Covers the common phrasings ("declined", "rejected", "not approved");
                    // if the real backend wording doesn't hit one of these, this falls through to
                    // the generic error branch below instead of misrouting.
                    when {
                        msg.contains("pending", ignoreCase = true) -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                navigateTo = LoginNavigation.PendingApproval
                            )
                        }
                        msg.contains("declined", ignoreCase = true) ||
                            msg.contains("rejected", ignoreCase = true) ||
                            msg.contains("not approved", ignoreCase = true) -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                navigateTo = LoginNavigation.RegistrationDeclined
                            )
                        }
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = LanguageManager.strings.friendlyError(msg)
                            )
                        }
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
