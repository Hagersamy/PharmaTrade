package com.pharmatrade.feature.pharmacyorder.presentation.orderdetail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
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
    val strings = LocalStrings.current

    LaunchedEffect(uiState.cancelled) {
        if (uiState.cancelled) onCancelled()
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(title = strings.orderDetailsTitle, onNavigateBack = onNavigateBack)

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
                val canDeliver = order.status.equals("delivered", ignoreCase = true)
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
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

                        order.supplierOrders.forEachIndexed { index, supplierOrder ->
                            SupplierOrderDetailCard(
                                supplierOrder = supplierOrder,
                                // Only the first supplier (or one still needing attention) opens by
                                // default — with several suppliers on one order, expanding all of them
                                // at once turned the screen into one long wall of line items.
                                initiallyExpanded = index == 0 || supplierOrder.status.equals("partially_available", ignoreCase = true)
                            )
                        }
                    }

                    // Kept outside the scrolling column (not just appended after the last card) so
                    // "Confirm delivery"/"Cancel order" stay reachable without scrolling past every
                    // supplier's line items on a multi-supplier order.
                    if (canDeliver || order.canCancel || uiState.actionError != null) {
                        Surface(shadowElevation = 8.dp, color = SurfaceWhite) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.actionError != null) {
                                    Text(uiState.actionError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                                }

                                // The backend only accepts confirm-delivery once the supplier has
                                // already marked the order "delivered" — it rejects any other status
                                // with "Order not found or not in delivered status."
                                if (canDeliver) {
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
                                            Text(strings.orderConfirmDelivery)
                                        }
                                    }
                                }

                                if (order.canCancel) {
                                    PharmaOutlinedButton(
                                        text = strings.orderCancelOrder,
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
        }
    }
}

// Statuses that participate in the normal, linear happy path — draft/cancelled/partially_available
// are exceptional states, not "further along" or "behind" on this line, so they don't get a step.
private val orderFlowStatusKeys = listOf(
    "pending_supplier_confirmation",
    "confirmed",
    "shipped",
    "delivered"
)

@Composable
private fun orderFlowStatuses(): List<Pair<String, String>> {
    val strings = LocalStrings.current
    val labels = listOf(strings.statusPending, strings.statusConfirmed, strings.statusShipped, strings.statusDelivered)
    return orderFlowStatusKeys.zip(labels)
}

@Composable
private fun OrderHeaderCard(order: OrderDetail) {
    val strings = LocalStrings.current
    val flowStatuses = orderFlowStatuses()
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

            val flowIndex = flowStatuses.indexOfFirst { it.first == order.status.lowercase() }
            if (flowIndex >= 0) {
                Spacer(Modifier.height(16.dp))
                OrderProgressStepper(currentIndex = flowIndex, flowStatuses = flowStatuses)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerGray)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.commonTotal, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
private fun OrderProgressStepper(currentIndex: Int, flowStatuses: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            flowStatuses.forEachIndexed { index, _ ->
                val reached = index <= currentIndex
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (reached) PrimaryBlue else DividerGray)
                )
                if (index != flowStatuses.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (index < currentIndex) PrimaryBlue else DividerGray)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            flowStatuses.forEachIndexed { index, (_, label) ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index <= currentIndex) TextPrimary else TextHint,
                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
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
    val strings = LocalStrings.current
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
                strings.orderShortageReportedMessage(
                    if (itemCount > 0) strings.orderShortageItemCount(itemCount) else strings.orderShortageSomeItems
                ),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = WarningAmberOnContainer
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancelMissingItems, enabled = !isSubmitting, modifier = Modifier.weight(1f)) {
                Text(strings.orderCancelMissingItems, style = MaterialTheme.typography.labelMedium)
            }
            Button(onClick = onWaitForAlternatives, enabled = !isSubmitting, modifier = Modifier.weight(1f)) {
                Text(strings.orderWaitForAlternatives, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SupplierOrderDetailCard(supplierOrder: SupplierOrder, initiallyExpanded: Boolean) {
    var expanded by remember(supplierOrder.supplierId) { mutableStateOf(initiallyExpanded) }
    val strings = LocalStrings.current

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
                    contentDescription = if (expanded) strings.commonCollapse else strings.commonExpand,
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
    val strings = LocalStrings.current
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
                strings.orderRequestedConfirmed(line.qtyRequested, line.qtyConfirmed?.toString() ?: "—"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                strings.orderPriceToTotal(formatDecimal(line.unitPrice, 2), formatDecimal(line.lineTotal, 2)),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
