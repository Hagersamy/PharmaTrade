package com.pharmatrade.feature.auth.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.pharmatrade.core.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToSellerDashboard: () -> Unit,
    onNavigateToBuyerCatalog: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onNavigateToPendingApproval: () -> Unit,
    onNavigateToRegistrationDeclined: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val strings = LocalStrings.current

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(PrimaryBlue, PrimaryBlueDark),
                    endY = 400f
                )
            )
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
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MedicalServices,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = strings.appTitle,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
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

                    if (uiState.error != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ErrorRedContainer)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline, null,
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = uiState.error!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRed
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    PharmaButton(
                        text = strings.loginSignIn,
                        onClick = viewModel::login,
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = uiState.isLoading
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
