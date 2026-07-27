package com.pharmatrade.feature.auth.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.io.rememberFilePickerLauncher
import com.pharmatrade.core.ui.components.*
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.auth.domain.model.Zone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPendingApproval: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val launchLicenceFrontPicker = rememberFilePickerLauncher(
        mimeTypes = listOf("image/*")
    ) { uri, _ -> viewModel.onLicenceFrontSelected(uri) }

    val launchLicenceBackPicker = rememberFilePickerLauncher(
        mimeTypes = listOf("image/*")
    ) { uri, _ -> viewModel.onLicenceBackSelected(uri) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateToPendingApproval()
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(title = "Create Account", onNavigateBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Role selector ─────────────────────────────────────────────────
            Text(
                text = "I am a...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UserTypeCard(
                    type = UserType.SELLER,
                    isSelected = uiState.userType == UserType.SELLER,
                    onClick = { viewModel.onUserTypeChange(UserType.SELLER) },
                    modifier = Modifier.weight(1f)
                )
                UserTypeCard(
                    type = UserType.BUYER,
                    isSelected = uiState.userType == UserType.BUYER,
                    onClick = { viewModel.onUserTypeChange(UserType.BUYER) },
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(color = DividerGray)

            // ── Account details ───────────────────────────────────────────────
            Text(
                text = "Account Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            PharmaTextField(value = uiState.name, onValueChange = viewModel::onNameChange, label = "Full Name", leadingIcon = Icons.Filled.Person)
            PharmaTextField(
                value = uiState.businessName,
                onValueChange = viewModel::onBusinessNameChange,
                label = if (uiState.userType == UserType.SELLER) "Business / Company Name" else "Pharmacy Name",
                leadingIcon = Icons.Filled.Business
            )
            PharmaTextField(value = uiState.email, onValueChange = viewModel::onEmailChange, label = "Email Address", leadingIcon = Icons.Filled.Email)
            PharmaTextField(value = uiState.phone, onValueChange = viewModel::onPhoneChange, label = "Phone Number", leadingIcon = Icons.Filled.Phone)

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, null, tint = TextSecondary) },
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                },
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DividerGray,
                    focusedLabelColor = PrimaryBlue
                )
            )

            val passwordMismatch = uiState.confirmPassword.isNotEmpty() && uiState.confirmPassword != uiState.password
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, null, tint = if (passwordMismatch) ErrorRed else TextSecondary) },
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isConfirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = if (passwordMismatch) ErrorRed else TextSecondary
                        )
                    }
                },
                visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = passwordMismatch,
                supportingText = if (passwordMismatch) { { Text("Passwords do not match", color = ErrorRed) } } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (passwordMismatch) ErrorRed else PrimaryBlue,
                    unfocusedBorderColor = if (passwordMismatch) ErrorRed else DividerGray,
                    focusedLabelColor = if (passwordMismatch) ErrorRed else PrimaryBlue,
                    errorBorderColor = ErrorRed,
                    errorLabelColor = ErrorRed
                )
            )

            Divider(color = DividerGray)

            // ── Business details ──────────────────────────────────────────────
            Text(
                text = if (uiState.userType == UserType.SELLER) "Supplier Details" else "Pharmacy Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            PharmaTextField(value = uiState.licenceNumber, onValueChange = viewModel::onLicenceNumberChange, label = "Licence Number", leadingIcon = Icons.Filled.Badge)

            // ── Zone multi-select ─────────────────────────────────────────────
            ZoneMultiSelectDropdown(
                zones = uiState.zones,
                selectedZones = uiState.selectedZones,
                isLoading = uiState.isLoadingZones,
                hasError = uiState.zonesError != null,
                onToggle = viewModel::onZoneToggled,
                onRetry = viewModel::loadZones
            )

            PharmaTextField(value = uiState.address, onValueChange = viewModel::onAddressChange, label = "Address", leadingIcon = Icons.Filled.LocationOn, singleLine = false)

            Divider(color = DividerGray)

            // ── Licence images ────────────────────────────────────────────────
            Text(text = "Licence Images", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)

            Text(text = "Front of licence", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            LicenseImagePicker(
                selectedUri = uiState.licenceFrontUri,
                onPickImage = launchLicenceFrontPicker,
                onRemoveImage = { viewModel.onLicenceFrontSelected(null) }
            )

            Text(text = "Back of licence", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            LicenseImagePicker(
                selectedUri = uiState.licenceBackUri,
                onPickImage = launchLicenceBackPicker,
                onRemoveImage = { viewModel.onLicenceBackSelected(null) }
            )

            // ── Supplier-only fields ──────────────────────────────────────────
            if (uiState.userType == UserType.SELLER) {
                Divider(color = DividerGray)
                Text(text = "Order Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)

                PharmaTextField(
                    value = uiState.minOrderValue,
                    onValueChange = viewModel::onMinOrderValueChange,
                    label = "Minimum Order Value (EGP)",
                    leadingIcon = Icons.Filled.Payments,
                    keyboardType = KeyboardType.Decimal
                )
                PharmaTextField(
                    value = uiState.minOrderQty,
                    onValueChange = viewModel::onMinOrderQtyChange,
                    label = "Minimum Order Quantity",
                    leadingIcon = Icons.Filled.Inventory2,
                    keyboardType = KeyboardType.Number
                )
            }

            // ── Error banner ──────────────────────────────────────────────────
            if (uiState.error != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ErrorRedContainer).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.error!!, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
            }

            PharmaButton(text = "Create Account", onClick = viewModel::register, modifier = Modifier.fillMaxWidth(), isLoading = uiState.isLoading)

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Licence image picker ──────────────────────────────────────────────────────

@Composable
private fun LicenseImagePicker(
    selectedUri: String?,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selectedUri != null) 2.dp else 1.5.dp,
                color = if (selectedUri != null) PrimaryBlue else DividerGray,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (selectedUri != null) PrimaryBlueContainer else SurfaceWhite)
            .clickable(onClick = onPickImage)
    ) {
        if (selectedUri != null) {
            AsyncImage(
                model = selectedUri,
                contentDescription = "Pharmacy license",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
            )
            IconButton(
                onClick = onRemoveImage,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Remove image", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.FileUpload, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text("Tap to upload license photo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = PrimaryBlue)
                Text("JPG, PNG accepted", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

// ── Zone multi-select dropdown ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ZoneMultiSelectDropdown(
    zones: List<Zone>,
    selectedZones: List<Zone>,
    isLoading: Boolean,
    hasError: Boolean,
    onToggle: (Zone) -> Unit,
    onRetry: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val grouped = remember(zones) { zones.groupBy { it.governorate ?: "Other" } }
    val selectedIds = remember(selectedZones) { selectedZones.map { it.id }.toSet() }

    val fieldText = when {
        isLoading -> "Loading zones..."
        selectedZones.isNotEmpty() -> selectedZones.joinToString(", ") { it.name }
        else -> ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = fieldText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Zones *") },
                placeholder = { Text("Select one or more zones", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.LocationCity, null, tint = TextSecondary) },
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                    } else {
                        val rotation by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f, label = "arrow"
                        )
                        Icon(Icons.Filled.ArrowDropDown, null, Modifier.rotate(rotation))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = if (expanded) PrimaryBlue else DividerGray,
                    focusedLabelColor = PrimaryBlue,
                    disabledBorderColor = if (expanded) PrimaryBlue else DividerGray,
                    disabledLabelColor = if (expanded) PrimaryBlue else TextSecondary,
                    disabledTextColor = TextPrimary,
                    disabledLeadingIconColor = TextSecondary,
                    disabledTrailingIconColor = TextSecondary
                ),
                enabled = false
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = !isLoading && zones.isNotEmpty()) { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 360.dp)
            ) {
                grouped.forEach { (governorate, zonesInGroup) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                governorate.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        },
                        onClick = {},
                        enabled = false,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    )
                    zonesInGroup.forEach { zone ->
                        val isSelected = zone.id in selectedIds
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                    )
                                    Text(
                                        zone.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) PrimaryBlue else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = { onToggle(zone) },   // keep menu open so more zones can be picked
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        )
                    }
                    HorizontalDivider(color = DividerGray, modifier = Modifier.padding(horizontal = 12.dp))
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            "Done",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    onClick = { expanded = false },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        if (selectedZones.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedZones.forEach { zone ->
                    InputChip(
                        selected = true,
                        onClick = { onToggle(zone) },
                        label = { Text(zone.name, style = MaterialTheme.typography.labelMedium) },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove ${zone.name}", modifier = Modifier.size(16.dp)) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = PrimaryBlueContainer,
                            selectedLabelColor = PrimaryBlue,
                            selectedTrailingIconColor = PrimaryBlue
                        )
                    )
                }
            }
        }

        if (hasError) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                Text("Could not load zones.", style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                    Text("Retry", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ── Role selector card ────────────────────────────────────────────────────────

@Composable
private fun UserTypeCard(
    type: UserType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = if (type == UserType.SELLER) Icons.Filled.LocalPharmacy else Icons.Filled.MedicalServices
    val label = if (type == UserType.SELLER) "Seller Agent" else "Pharmacy"
    val description = if (type == UserType.SELLER) "Distribute drugs" else "Buy drugs"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PrimaryBlue else DividerGray,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isSelected) PrimaryBlueContainer else SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) PrimaryBlue else TextSecondary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (isSelected) PrimaryBlue else TextPrimary)
            Text(text = description, style = MaterialTheme.typography.labelSmall, color = if (isSelected) PrimaryBlue else TextSecondary)
        }
    }
}
