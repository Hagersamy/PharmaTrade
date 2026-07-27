package com.pharmatrade.feature.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.admin.domain.model.PendingUser

@Composable
fun AnalyticsContent(
    viewModel: AnalyticsViewModel,
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Stats section ─────────────────────────────────────────────────────
        item {
            Text(
                text = "Registration Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        item {
            if (uiState.isLoadingStats) {
                Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AnalyticsStatCard(
                            icon = Icons.Filled.HourglassTop,
                            label = "Pending",
                            value = uiState.stats.pending.toString(),
                            tint = WarningAmber,
                            bg = WarningAmberContainer,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsStatCard(
                            icon = Icons.Filled.CheckCircle,
                            label = "Approved",
                            value = uiState.stats.approved.toString(),
                            tint = SecondaryGreen,
                            bg = SecondaryGreenContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AnalyticsStatCard(
                            icon = Icons.Filled.Cancel,
                            label = "Declined",
                            value = uiState.stats.declined.toString(),
                            tint = ErrorRed,
                            bg = ErrorRedContainer,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsStatCard(
                            icon = Icons.Filled.Group,
                            label = "Total",
                            value = uiState.stats.total.toString(),
                            tint = PrimaryBlue,
                            bg = PrimaryBlueContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (uiState.stats.pendingPharmacies > 0 || uiState.stats.pendingSuppliers > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AnalyticsStatCard(
                                icon = Icons.Filled.LocalPharmacy,
                                label = "Pending Pharmacies",
                                value = uiState.stats.pendingPharmacies.toString(),
                                tint = PrimaryBlue,
                                bg = PrimaryBlueContainer,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsStatCard(
                                icon = Icons.Filled.Inventory,
                                label = "Pending Suppliers",
                                value = uiState.stats.pendingSuppliers.toString(),
                                tint = WarningAmber,
                                bg = WarningAmberContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // ── Filter tabs ───────────────────────────────────────────────────────
        item {
            Text(
                text = "Pending Requests",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EntityFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // ── Requests list ─────────────────────────────────────────────────────
        if (uiState.isLoadingRequests) {
            item {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
        } else if (uiState.error != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(36.dp))
                    Text(uiState.error!!, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }
        } else if (uiState.requests.isEmpty()) {
            item {
                EmptyState(
                    title = "No Pending Requests",
                    message = "All registration requests have been handled",
                    icon = Icons.Filled.CheckCircle,
                    action = {
                        OutlinedButton(onClick = viewModel::refresh) {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Refresh")
                        }
                    }
                )
            }
        } else {
            items(uiState.requests, key = { it.id }) { request ->
                AnalyticsPendingCard(
                    user = request,
                    onApprove = { notes -> viewModel.approveRequest(request.id, notes) },
                    onDecline = { reason -> viewModel.declineRequest(request.id, reason) }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Stat card ─────────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

// ── Analytics pending card ────────────────────────────────────────────────────

@Composable
private fun AnalyticsPendingCard(
    user: PendingUser,
    onApprove: (notes: String) -> Unit,
    onDecline: (reason: String) -> Unit
) {
    var showApproveDialog by remember { mutableStateOf(false) }
    var showDeclineDialog by remember { mutableStateOf(false) }

    if (showApproveDialog) {
        ApproveDialog(
            userName = user.name,
            onConfirm = { notes -> showApproveDialog = false; onApprove(notes) },
            onDismiss = { showApproveDialog = false }
        )
    }

    if (showDeclineDialog) {
        DeclineDialog(
            userName = user.name,
            onConfirm = { reason -> showDeclineDialog = false; onDecline(reason) },
            onDismiss = { showDeclineDialog = false }
        )
    }

    val isPharmacy = user.userType == UserType.BUYER
    val accentColor = if (isPharmacy) PrimaryBlue else WarningAmber
    val accentBg    = if (isPharmacy) PrimaryBlueContainer else WarningAmberContainer
    val typeLabel   = if (isPharmacy) "Pharmacy" else "Supplier"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = user.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = user.businessName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = accentBg) {
                    Text(
                        text = typeLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }

            // Details
            Spacer(Modifier.height(12.dp))
            Divider(color = DividerGray)
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AnalyticsInfoRow(Icons.Filled.Phone, "Phone", user.phone)
                if (user.email.isNotBlank()) AnalyticsInfoRow(Icons.Filled.Email, "Email", user.email)
                user.licenceNumber?.takeIf { it.isNotBlank() }?.let { AnalyticsInfoRow(Icons.Filled.Badge, "Licence", it) }
                user.zoneId?.takeIf { it.isNotBlank() }?.let { AnalyticsInfoRow(Icons.Filled.LocationCity, "Zone", it) }
                user.address?.takeIf { it.isNotBlank() }?.let { AnalyticsInfoRow(Icons.Filled.LocationOn, "Address", it) }
                if (user.userType == UserType.SELLER) {
                    if (!user.minOrderValue.isNullOrBlank()) AnalyticsInfoRow(Icons.Filled.Payments, "Min Order", "EGP ${user.minOrderValue}")
                    if (user.additionalZoneIds.isNotEmpty()) AnalyticsInfoRow(Icons.Filled.Map, "Extra Zones", user.additionalZoneIds.joinToString(", "))
                }
            }

            // Licence images
            if (!user.licenceFrontUrl.isNullOrBlank() || !user.licenceBackUrl.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsLicenceImage(url = user.licenceFrontUrl, label = "Front", modifier = Modifier.weight(1f))
                    AnalyticsLicenceImage(url = user.licenceBackUrl, label = "Back", modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showDeclineDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                ) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Decline", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { showApproveDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Approve", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Approve dialog (optional notes) ──────────────────────────────────────────

@Composable
private fun ApproveDialog(
    userName: String,
    onConfirm: (notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.CheckCircle, null, tint = SecondaryGreen) },
        title = { Text("Approve $userName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "The user will be notified and granted access to the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("e.g. Licence verified. Approved.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryGreen,
                        focusedLabelColor = SecondaryGreen
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes) },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen)
            ) { Text("Approve") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Decline dialog (required reason) ─────────────────────────────────────────

@Composable
private fun DeclineDialog(
    userName: String,
    onConfirm: (reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val isValid = reason.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Cancel, null, tint = ErrorRed) },
        title = { Text("Decline $userName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "The user will be informed of the reason for declining their request.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason *") },
                    placeholder = { Text("e.g. Licence image is unclear. Please resubmit.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 3,
                    isError = reason.isEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ErrorRed,
                        focusedLabelColor = ErrorRed
                    )
                )
                if (!isValid) {
                    Text("A reason is required", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onConfirm(reason) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) { Text("Decline") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(15.dp).padding(top = 1.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AnalyticsLicenceImage(url: String?, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(10.dp)).background(CardGray),
            contentAlignment = Alignment.Center
        ) {
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = "Licence $label",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                )
            } else {
                Icon(Icons.Filled.Image, null, tint = TextHint, modifier = Modifier.size(28.dp))
            }
        }
    }
}
