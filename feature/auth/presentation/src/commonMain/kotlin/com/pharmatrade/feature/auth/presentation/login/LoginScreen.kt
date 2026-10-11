package com.pharmatrade.feature.auth.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaTextField
import com.pharmatrade.core.ui.components.rememberToast
import com.pharmatrade.core.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToSellerDashboard: () -> Unit,
    onNavigateToBuyerCatalog: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onNavigateToPendingApproval: () -> Unit,
    onNavigateToRegistrationDeclined: () -> Unit,
    // The platform's app icon (Android launcher icon), supplied by the app module since
    // launcher resources aren't reachable from shared code. Null shows no logo.
    appLogo: Painter? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val strings = LocalStrings.current
    val showLongToast = rememberToast(long = true)

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            showLongToast(it)
            viewModel.onErrorShown()
        }
    }

    LaunchedEffect(uiState.navigateTo) {
        when (uiState.navigateTo) {
            is LoginNavigation.SellerDashboard -> {
                viewModel.onNavigationHandled()
                onNavigateToSellerDashboard()
            }
            is LoginNavigation.BuyerCatalog -> {
                viewModel.onNavigationHandled()
                onNavigateToBuyerCatalog()
            }
            is LoginNavigation.AdminDashboard -> {
                viewModel.onNavigationHandled()
                onNavigateToAdminDashboard()
            }
            is LoginNavigation.PendingApproval -> {
                viewModel.onNavigationHandled()
                onNavigateToPendingApproval()
            }
            is LoginNavigation.RegistrationDeclined -> {
                viewModel.onNavigationHandled()
                onNavigateToRegistrationDeclined()
            }
            null -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Same blue as the app icon, so the logo sits seamlessly on the background.
            .background(BrandBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo area
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (appLogo != null) {
                    Image(
                        painter = appLogo,
                        contentDescription = strings.appTitle,
                        modifier = Modifier.size(96.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    text = strings.loginTagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(40.dp))

            // Login card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = strings.loginWelcomeBack,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = strings.loginSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(24.dp))

                    PharmaTextField(
                        value = uiState.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = strings.loginPhoneLabel,
                        leadingIcon = Icons.Filled.Phone,
                        keyboardType = KeyboardType.Phone
                    )

                    Spacer(Modifier.height(16.dp))

                    PharmaTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = strings.loginPasswordLabel,
                        leadingIcon = Icons.Filled.Lock,
                        trailingIcon = {
                            IconButton(onClick = viewModel::togglePasswordVisibility) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible)
                                        Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (uiState.isPasswordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            focusManager.clearFocus()
                            viewModel.login()
                        }
                    )

                    Spacer(Modifier.height(24.dp))

                    PharmaButton(
                        text = strings.loginSignIn,
                        onClick = viewModel::login,
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = uiState.isLoading,
                        containerColor = BrandBlue
                    )

                }
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.loginNoAccount, color = Color.White.copy(alpha = 0.8f))
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = strings.loginRegister,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
