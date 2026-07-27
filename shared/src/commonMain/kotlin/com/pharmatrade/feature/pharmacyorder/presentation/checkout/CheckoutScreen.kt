package com.pharmatrade.feature.pharmacyorder.presentation.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.DiscountBadge
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderDrugLine

@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(title = "Checkout", onNavigateBack = onNavigateBack)

        if (uiState.orders.isEmpty()) {
            EmptyState(
                title = "Nothing left to check out",
                message = "You removed every order — go back to your cart to add more",
                icon = Icons.Filled.ReceiptLong,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "EGP ${formatDecimal(uiState.grandTotal, 2)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "Across ${uiState.orders.size} order${if (uiState.orders.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                uiState.orders.forEach { orderState ->
                    CheckoutOrderCard(state = orderState, onRemove = { viewModel.removeOrder(orderState.orderId) })
                }
            }

            Surface(shadowElevation = 8.dp, color = SurfaceWhite) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PharmaButton(
                        text = "Done",
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutOrderCard(state: CheckoutOrderState, onRemove: () -> Unit) {
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            when {
                state.isLoading -> Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                    Text("Submitting order…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                state.error != null -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(state.error, style = MaterialTheme.typography.bodySmall, color = ErrorRed, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRemove, enabled = !state.isCancelling) { Text("Remove") }
                }
                state.order != null -> {
                    val order = state.order
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            order.supplierOrders.forEach { supplierOrder ->
                                Text(supplierOrder.supplierName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text(
                                "EGP ${formatDecimal(order.totalValue, 2)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = onRemove, enabled = !state.isCancelling) {
                            if (state.isCancelling) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = ErrorRed)
                            } else {
                                Icon(Icons.Filled.Close, contentDescription = "Remove this order", tint = ErrorRed)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = DividerGray)
                    Spacer(Modifier.height(8.dp))
                    order.supplierOrders.forEach { supplierOrder ->
                        supplierOrder.lines.forEach { line -> CheckoutDrugLineRow(line) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutDrugLineRow(line: OrderDrugLine) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(line.drugName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                DiscountBadge(discountPercentage = line.discountPct)
            }
            Text(
                "Qty ${line.qtyConfirmed ?: line.qtyRequested} × EGP ${formatDecimal(line.unitPrice, 2)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Text(
            "EGP ${formatDecimal(line.lineTotal, 2)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
