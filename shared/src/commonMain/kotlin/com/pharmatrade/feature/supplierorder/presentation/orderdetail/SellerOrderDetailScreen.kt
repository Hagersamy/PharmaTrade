package com.pharmatrade.feature.supplierorder.presentation.orderdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.util.formatBackendTimestamp
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.PharmaOutlinedButton
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.presentation.common.OrderStatusChip
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderDetail
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderItem

@Composable
fun SellerOrderDetailScreen(
    viewModel: SellerOrderDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onSnackbarShown()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundGray)
        ) {
            PharmaTopBar(title = "Order Details", onNavigateBack = onNavigateBack)

            when {
                uiState.isLoading && uiState.order == null -> Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
                uiState.error != null && uiState.order == null -> ErrorScreen(
                    message = uiState.error!!,
                    onRetry = viewModel::loadOrder,
                    modifier = Modifier.weight(1f)
                )
                uiState.order != null -> {
                    val order = uiState.order!!
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OrderHeaderCard(order)
                        PharmacyInfoCard(order)

                        order.items.forEach { item -> OrderItemCard(item) }

                        if (uiState.actionError != null) {
                            Text(uiState.actionError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                        }

                        ActionButtons(
                            order = order,
                            isSubmitting = uiState.isSubmitting,
                            onConfirm = viewModel::openConfirmDialog,
                            onReportShortage = viewModel::openShortageDialog,
                            onShip = viewModel::ship,
                            onDeliver = viewModel::deliver
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (uiState.showConfirmDialog && uiState.order != null) {
        ConfirmOrderDialog(
            items = uiState.order!!.items,
            quantities = uiState.confirmQuantities,
            isSubmitting = uiState.isSubmitting,
            error = uiState.actionError,
            onQuantityChange = viewModel::onConfirmQuantityChange,
            onConfirm = viewModel::submitConfirm,
            onDismiss = viewModel::dismissConfirmDialog
        )
    }

    if (uiState.showShortageDialog && uiState.order != null) {
        ReportShortageDialog(
            items = uiState.order!!.items,
            quantities = uiState.shortageQuantities,
            notes = uiState.shortageNotes,
            isSubmitting = uiState.isSubmitting,
            error = uiState.actionError,
            onQuantityChange = viewModel::onShortageQuantityChange,
            onNotesChange = viewModel::onShortageNotesChange,
            onSubmit = viewModel::submitShortage,
            onDismiss = viewModel::dismissShortageDialog
        )
    }
}

@Composable
private fun OrderHeaderCard(order: SupplierOrderDetail) {
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(order.orderNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    order.confirmedAt?.let {
                        Text("Confirmed ${formatBackendTimestamp(it)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                    }
                    order.shippedAt?.let {
                        Text("Shipped ${formatBackendTimestamp(it)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                    }
                    order.deliveredAt?.let {
                        Text("Delivered ${formatBackendTimestamp(it)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                    }
                }
                OrderStatusChip(status = order.status)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerGray)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    "EGP ${formatDecimal(order.subtotal, 2)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Platform commission: ${formatDecimal(order.commissionPct, 1)}% (EGP ${formatDecimal(order.commissionValue, 2)})",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PharmacyInfoCard(order: SupplierOrderDetail) {
    if (order.pharmacyBranchName.isBlank()) return
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pharmacy", style = MaterialTheme.typography.labelSmall, color = TextHint)
            Spacer(Modifier.height(4.dp))
            Text(order.pharmacyBranchName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            if (order.pharmacyBranchAddress.isNotBlank()) {
                Text(order.pharmacyBranchAddress, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (order.pharmacyBranchPhone.isNotBlank()) {
                Text(order.pharmacyBranchPhone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun OrderItemCard(item: SupplierOrderItem) {
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.drugName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            val subtitle = listOfNotNull(item.dosageForm?.takeIf { it.isNotBlank() }, item.strength?.takeIf { it.isNotBlank() }).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Requested ${item.quantityRequested} · Confirmed ${item.quantityConfirmed ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    "EGP ${formatDecimal(item.unitPrice, 2)}/unit → EGP ${formatDecimal(item.lineTotal, 2)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    order: SupplierOrderDetail,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onReportShortage: () -> Unit,
    onShip: () -> Unit,
    onDeliver: () -> Unit
) {
    when {
        order.canConfirm -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PharmaButton(text = "Confirm Order", onClick = onConfirm, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth())
            PharmaOutlinedButton(text = "Report Shortage", onClick = onReportShortage, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth())
        }
        order.canShip -> PharmaButton(
            text = "Mark as Shipped",
            onClick = onShip,
            isLoading = isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
        order.canDeliver -> PharmaButton(
            text = "Mark as Delivered",
            onClick = onDeliver,
            isLoading = isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConfirmOrderDialog(
    items: List<SupplierOrderItem>,
    quantities: Map<String, String>,
    isSubmitting: Boolean,
    error: String?,
    onQuantityChange: (String, String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        icon = { Icon(Icons.Filled.LocalShipping, null, tint = PrimaryBlue) },
        title = { Text("Confirm Order", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Enter how many units you can fulfil for each item.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                items.forEach { item ->
                    Column {
                        Text(item.drugName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = quantities[item.id] ?: item.quantityRequested.toString(),
                            onValueChange = { onQuantityChange(item.id, it) },
                            label = { Text("Confirmed qty (requested ${item.quantityRequested})") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isSubmitting) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Confirm")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReportShortageDialog(
    items: List<SupplierOrderItem>,
    quantities: Map<String, String>,
    notes: String,
    isSubmitting: Boolean,
    error: String?,
    onQuantityChange: (String, String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        icon = { Icon(Icons.Filled.Warning, null, tint = WarningAmber) },
        title = { Text("Report Shortage", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Enter how many units are actually available. The rest will be reported as short.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                items.forEach { item ->
                    Column {
                        Text(item.drugName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = quantities[item.id] ?: item.quantityRequested.toString(),
                            onValueChange = { onQuantityChange(item.id, it) },
                            label = { Text("Available qty (requested ${item.quantityRequested})") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (e.g. stock ran out at warehouse)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = !isSubmitting) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        }
    )
}
