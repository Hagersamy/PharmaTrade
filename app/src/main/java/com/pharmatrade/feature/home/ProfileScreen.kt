package com.pharmatrade.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaTextField
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.auth.domain.model.Zone
import com.pharmatrade.feature.pharmacyorder.domain.model.Branch
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, user: User?, onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(2500)
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.accountDeactivated) {
        if (uiState.accountDeactivated) {
            onLogout()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.successMessage != null) {
            Surface(shape = RoundedCornerShape(12.dp), color = SecondaryGreenContainer) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SecondaryGreenDark)
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.successMessage!!, color = SecondaryGreenDark, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Avatar + name header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = user?.name ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            val badgeBg = when (user?.userType) {
                UserType.SELLER -> PrimaryBlueContainer
                UserType.ADMIN  -> WarningAmberContainer
                else            -> SecondaryGreenContainer
            }
            val badgeFg = when (user?.userType) {
                UserType.SELLER -> PrimaryBlue
                UserType.ADMIN  -> WarningAmber
                else            -> SecondaryGreenDark
            }
            Surface(shape = RoundedCornerShape(16.dp), color = badgeBg) {
                Text(
                    text = user?.userType?.displayName ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeFg,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Account info card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Account Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = viewModel::openEditProfileDialog, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit profile", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                }
                Divider(color = DividerGray)
                ProfileRow(Icons.Filled.Email, "Email", user?.email ?: "—")
                ProfileRow(Icons.Filled.Phone, "Phone", user?.phone ?: "—")
                ProfileRow(
                    Icons.Filled.Business,
                    "Business Name",
                    user?.businessName.takeIf { !it.isNullOrBlank() } ?: "—"
                )
                if (!user?.licenceNumber.isNullOrBlank()) {
                    ProfileRow(Icons.Filled.Badge, "Licence Number", user?.licenceNumber!!)
                }
                if (!user?.address.isNullOrBlank()) {
                    ProfileRow(Icons.Filled.LocationOn, "Address", user?.address!!)
                }
                TextButton(onClick = viewModel::openChangePasswordDialog, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Change Password", color = PrimaryBlue)
                }
            }
        }

        if (user?.userType == UserType.SELLER) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Supplier Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = viewModel::openEditSupplierDialog, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit supplier info", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                    Divider(color = DividerGray)
                    if (!user.minOrderValue.isNullOrBlank()) {
                        ProfileRow(Icons.Filled.Payments, "Min Order Value", "EGP ${user.minOrderValue}")
                    }
                    if (!user.minOrderQty.isNullOrBlank()) {
                        ProfileRow(Icons.Filled.Inventory2, "Min Order Qty", user.minOrderQty!!)
                    }
                    if (uiState.supplierZones.isNotEmpty()) {
                        Column {
                            Text("Delivery Zones", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            ZoneChipsRow(uiState.supplierZones.map { it.name })
                        }
                    } else if (!user.zoneId.isNullOrBlank()) {
                        ProfileRow(Icons.Filled.LocationCity, "Primary Zone", user.zoneId!!)
                    }
                    TextButton(onClick = viewModel::openRequestZoneDialog, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Filled.EditLocationAlt, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Request Zone Change", color = PrimaryBlue)
                    }
                }
            }
        }

        if (user?.userType == UserType.BUYER) {
            BranchLocationCard(
                branch = uiState.branch,
                isLoading = uiState.isLoadingBranch,
                error = uiState.branchError,
                onRetry = viewModel::loadBranch,
                onEdit = viewModel::openEditBranchDialog,
                onRequestZoneChange = viewModel::openRequestZoneDialog
            )
        }

        Spacer(Modifier.height(8.dp))

        PharmaButton(
            text = "Sign Out",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            containerColor = ErrorRed
        )

        TextButton(
            onClick = viewModel::openDeactivateDialog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Deactivate Account", color = ErrorRed)
        }
    }

    if (uiState.showEditProfileDialog) {
        EditProfileDialog(uiState, viewModel)
    }
    if (uiState.showChangePasswordDialog) {
        ChangePasswordDialog(uiState, viewModel)
    }
    if (uiState.showEditSupplierDialog) {
        EditSupplierDialog(uiState, viewModel)
    }
    if (uiState.showEditBranchDialog) {
        EditBranchDialog(uiState, viewModel)
    }
    if (uiState.showDeactivateDialog) {
        DeactivateAccountDialog(uiState, viewModel)
    }
    if (uiState.showRequestZoneDialog) {
        RequestZoneDialog(uiState, viewModel)
    }
}

@Composable
private fun BranchLocationCard(
    branch: Branch?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onRequestZoneChange: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Branch & Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit branch info", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                }
            }
            Divider(color = DividerGray)

            when {
                isLoading && branch == null -> Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                }
                error != null && branch == null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Could not load branch details.", style = MaterialTheme.typography.bodySmall, color = ErrorRed, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) { Text("Retry") }
                }
                branch != null -> {
                    ProfileRow(Icons.Filled.Storefront, "Branch Name", branch.name.ifBlank { "—" })
                    if (branch.address.isNotBlank()) {
                        ProfileRow(Icons.Filled.LocationOn, "Address", branch.address)
                    }
                    if (branch.zones.isNotEmpty()) {
                        Column {
                            Text("Delivery Zones", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            ZoneChipsRow(branch.zones.map { it.name })
                        }
                    }
                    TextButton(onClick = onRequestZoneChange, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Filled.EditLocationAlt, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Request Zone Change", color = PrimaryBlue)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneChipsRow(zoneNames: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        zoneNames.forEach { zone ->
            Surface(shape = RoundedCornerShape(20.dp), color = PrimaryBlueContainer) {
                Text(
                    text = zone,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}

@Composable
private fun EditProfileDialog(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isSavingProfile) viewModel.dismissEditProfileDialog() },
        icon = { Icon(Icons.Filled.Edit, null, tint = PrimaryBlue) },
        title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PharmaTextField(
                    value = uiState.editName,
                    onValueChange = viewModel::onEditNameChange,
                    label = "Name",
                    leadingIcon = Icons.Filled.Person
                )
                PharmaTextField(
                    value = uiState.editEmail,
                    onValueChange = viewModel::onEditEmailChange,
                    label = "Email",
                    leadingIcon = Icons.Filled.Email,
                    keyboardType = KeyboardType.Email
                )
                PharmaTextField(
                    value = uiState.editPhone,
                    onValueChange = viewModel::onEditPhoneChange,
                    label = "Phone Number",
                    leadingIcon = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone
                )
                MaskedPasswordField(
                    value = uiState.editProfileConfirmPassword,
                    onValueChange = viewModel::onEditProfileConfirmPasswordChange,
                    label = "Password (to confirm)"
                )
                if (uiState.profileFormError != null) {
                    Text(uiState.profileFormError, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::saveProfile, enabled = !uiState.isSavingProfile) {
                if (uiState.isSavingProfile) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissEditProfileDialog, enabled = !uiState.isSavingProfile) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isSavingPassword) viewModel.dismissChangePasswordDialog() },
        icon = { Icon(Icons.Filled.Lock, null, tint = PrimaryBlue) },
        title = { Text("Change Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MaskedPasswordField(
                    value = uiState.currentPassword,
                    onValueChange = viewModel::onCurrentPasswordChange,
                    label = "Current Password"
                )
                MaskedPasswordField(
                    value = uiState.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    label = "New Password"
                )
                MaskedPasswordField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = "Confirm New Password"
                )
                if (uiState.passwordError != null) {
                    Text(uiState.passwordError, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::changePassword, enabled = !uiState.isSavingPassword) {
                if (uiState.isSavingPassword) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissChangePasswordDialog, enabled = !uiState.isSavingPassword) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditSupplierDialog(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isSavingSupplier) viewModel.dismissEditSupplierDialog() },
        icon = { Icon(Icons.Filled.Business, null, tint = PrimaryBlue) },
        title = { Text("Edit Supplier Info", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PharmaTextField(
                    value = uiState.editSupplierName,
                    onValueChange = viewModel::onEditSupplierNameChange,
                    label = "Business Name",
                    leadingIcon = Icons.Filled.Business
                )
                PharmaTextField(
                    value = uiState.editMinOrderValue,
                    onValueChange = viewModel::onEditMinOrderValueChange,
                    label = "Minimum Order Value (EGP)",
                    leadingIcon = Icons.Filled.Payments,
                    keyboardType = KeyboardType.Decimal
                )
                PharmaTextField(
                    value = uiState.editMinOrderQty,
                    onValueChange = viewModel::onEditMinOrderQtyChange,
                    label = "Minimum Order Quantity",
                    leadingIcon = Icons.Filled.Inventory2,
                    keyboardType = KeyboardType.Number
                )
                if (uiState.supplierError != null) {
                    Text(uiState.supplierError, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::saveSupplier, enabled = !uiState.isSavingSupplier) {
                if (uiState.isSavingSupplier) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissEditSupplierDialog, enabled = !uiState.isSavingSupplier) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditBranchDialog(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isSavingBranch) viewModel.dismissEditBranchDialog() },
        icon = { Icon(Icons.Filled.Storefront, null, tint = PrimaryBlue) },
        title = { Text("Edit Branch Info", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PharmaTextField(
                    value = uiState.editBranchName,
                    onValueChange = viewModel::onEditBranchNameChange,
                    label = "Branch Name",
                    leadingIcon = Icons.Filled.Storefront
                )
                PharmaTextField(
                    value = uiState.editBranchAddress,
                    onValueChange = viewModel::onEditBranchAddressChange,
                    label = "Address",
                    leadingIcon = Icons.Filled.LocationOn
                )
                PharmaTextField(
                    value = uiState.editBranchPhone,
                    onValueChange = viewModel::onEditBranchPhoneChange,
                    label = "Phone",
                    leadingIcon = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone
                )
                PharmaTextField(
                    value = uiState.editBranchLicence,
                    onValueChange = viewModel::onEditBranchLicenceChange,
                    label = "Licence Number",
                    leadingIcon = Icons.Filled.Badge
                )
                if (uiState.branchFormError != null) {
                    Text(uiState.branchFormError, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::saveBranch, enabled = !uiState.isSavingBranch) {
                if (uiState.isSavingBranch) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissEditBranchDialog, enabled = !uiState.isSavingBranch) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeactivateAccountDialog(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isDeactivating) viewModel.dismissDeactivateDialog() },
        icon = { Icon(Icons.Filled.Warning, null, tint = ErrorRed) },
        title = { Text("Deactivate Account?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "This will deactivate your account and sign you out. You won't be able to use the app until it's reactivated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (uiState.deactivateError != null) {
                    Text(uiState.deactivateError, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::deactivateAccount, enabled = !uiState.isDeactivating) {
                if (uiState.isDeactivating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ErrorRed)
                } else {
                    Text("Deactivate", color = ErrorRed)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissDeactivateDialog, enabled = !uiState.isDeactivating) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RequestZoneDialog(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isSubmittingZoneRequest) viewModel.dismissRequestZoneDialog() },
        icon = { Icon(Icons.Filled.EditLocationAlt, null, tint = PrimaryBlue) },
        title = { Text("Request Zone Change", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Select the delivery zones you'd like to cover. An admin will review this request.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                when {
                    uiState.isLoadingZones -> Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                    }
                    uiState.zonesError != null -> Text(
                        "Could not load zones: ${uiState.zonesError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                    else -> SelectableZoneChipsRow(
                        zones = uiState.availableZones,
                        selectedIds = uiState.selectedZones.map { it.id }.toSet(),
                        onToggle = viewModel::onZoneToggle
                    )
                }
                PharmaTextField(
                    value = uiState.zoneChangeReason,
                    onValueChange = viewModel::onZoneChangeReasonChange,
                    label = "Reason",
                    leadingIcon = Icons.Filled.Description
                )
                if (uiState.zoneRequestError != null) {
                    Text(uiState.zoneRequestError, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::submitZoneChangeRequest, enabled = !uiState.isSubmittingZoneRequest) {
                if (uiState.isSubmittingZoneRequest) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissRequestZoneDialog, enabled = !uiState.isSubmittingZoneRequest) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectableZoneChipsRow(zones: List<Zone>, selectedIds: Set<Int>, onToggle: (Zone) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        zones.forEach { zone ->
            val isSelected = zone.id in selectedIds
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) PrimaryBlue else CardGray,
                modifier = Modifier.clickable { onToggle(zone) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = zone.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MaskedPasswordField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary) },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = DividerGray,
            focusedLabelColor = PrimaryBlue,
            cursorColor = PrimaryBlue
        )
    )
}
