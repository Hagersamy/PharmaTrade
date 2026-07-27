package com.pharmatrade.feature.pharmacyorder.presentation.orderdetail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.util.formatBackendTimestamp
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.DiscountBadge
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.PharmaOutlinedButton
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderDetail
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderDrugLine
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierOrder
import com.pharmatrade.feature.pharmacyorder.presentation.common.OrderStatusChip

@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel,
    onNavigateBack: () -> Unit,
    onCancelled: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.cancelled) {
        if (uiState.cancelled) onCancelled()
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
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

                    if (order.hasUnresolvedShortage) {
                        ShortageBanner(
                            itemCount = order.shortageItemCount,
                            isSubmitting = uiState.isResolvingShortage,
                            // "cancel_short_items" is confirmed from the real API; "wait_for_alternatives"
                            // is not yet confirmed against a live shortage response — adjust if the
                            // backend expects a different value.
                            onCancelMissingItems = { viewModel.resolveShortage("cancel_short_items") },
                            onWaitForAlternatives = { viewModel.resolveShortage("wait_for_alternatives") }
                        )
                    }

                    order.supplierOrders.forEach { supplierOrder ->
                        SupplierOrderDetailCard(supplierOrder)
                    }

                    if (uiState.actionError != null) {
                        Text(uiState.actionError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }

                    // The backend only accepts confirm-delivery once the supplier has already
                    // marked the order "delivered" — it rejects any other status with
                    // "Order not found or not in delivered status."
                    if (order.status.equals("delivered", ignoreCase = true)) {
                        Button(
                            onClick = viewModel::deliver,
                            enabled = !uiState.isDelivering,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isDelivering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            } else {
                                Text("Confirm delivery")
                            }
                        }
                    }

                    if (order.canCancel) {
                        PharmaOutlinedButton(
                            text = "Cancel order",
                            onClick = viewModel::cancelOrder,
                            enabled = !uiState.isCancelling,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderHeaderCard(order: OrderDetail) {
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(order.orderNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(formatBackendTimestamp(order.date), style = MaterialTheme.typography.labelSmall, color = TextHint)
                }
                OrderStatusChip(status = order.status)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerGray)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    "EGP ${formatDecimal(order.totalValue, 2)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(order.platformFeeNote, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun ShortageBanner(
    itemCount: Int,
    isSubmitting: Boolean,
    onCancelMissingItems: () -> Unit,
    onWaitForAlternatives: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarningAmberContainer)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Shortage reported — ${if (itemCount > 0) "$itemCount items" else "some items"} could not be fulfilled.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color(0xFF92400E)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancelMissingItems, enabled = !isSubmitting, modifier = Modifier.weight(1f)) {
                Text("Cancel missing items", style = MaterialTheme.typography.labelMedium)
            }
            Button(onClick = onWaitForAlternatives, enabled = !isSubmitting, modifier = Modifier.weight(1f)) {
                Text("Wait for alternatives", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SupplierOrderDetailCard(supplierOrder: SupplierOrder) {
    var expanded by remember { mutableStateOf(true) }

    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.animateContentSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(supplierOrder.supplierName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "EGP ${formatDecimal(supplierOrder.subtotal, 2)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        OrderStatusChip(status = supplierOrder.status)
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary
                )
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = DividerGray)
                Spacer(Modifier.height(8.dp))
                supplierOrder.lines.forEach { line -> DrugLineDetailRow(line) }
            }
        }
    }
}

@Composable
private fun DrugLineDetailRow(line: OrderDrugLine) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(line.drugName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            DiscountBadge(discountPercentage = line.discountPct)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Requested ${line.qtyRequested} · Confirmed ${line.qtyConfirmed ?: "—"}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                "EGP ${formatDecimal(line.unitPrice, 2)}/unit → EGP ${formatDecimal(line.lineTotal, 2)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
