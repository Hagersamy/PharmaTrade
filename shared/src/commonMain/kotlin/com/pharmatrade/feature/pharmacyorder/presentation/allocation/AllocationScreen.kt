package com.pharmatrade.feature.pharmacyorder.presentation.allocation

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
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.DiscountBadge
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.PharmaOutlinedButton
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderDrugLine
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierOrder

@Composable
fun AllocationScreen(
    viewModel: AllocationViewModel,
    onNavigateBack: () -> Unit,
    onConfirmed: (orderId: String) -> Unit,
    onCancelled: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    LaunchedEffect(uiState.cancelled) {
        if (uiState.cancelled) onCancelled()
    }

    // Backing out here (top-bar arrow or system/gesture back) does NOT cancel the order — it
    // stays pending on the backend exactly as allocate() left it. The buyer can come back to it
    // later (e.g. from Active Orders) and explicitly Confirm or Cancel; only the "Cancel order"
    // button below actually cancels it.
    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(title = strings.allocationTitle, onNavigateBack = onNavigateBack)

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(Modifier.height(16.dp))
                    Text(strings.allocationFindingBestPrices, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            uiState.error != null && uiState.order == null -> ErrorScreen(
                message = uiState.error!!,
                onRetry = viewModel::allocate,
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
                    Text(
                        "EGP ${formatDecimal(order.totalValue, 2)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (order.savings > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SecondaryGreenContainer)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Savings, contentDescription = null, tint = SecondaryGreenDark, modifier = Modifier.size(16.dp))
                            Text(
                                strings.allocationYouSaved(formatDecimal(order.savings, 2)),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryGreenDark
                            )
                        }
                    }

                    order.supplierOrders.forEach { supplierOrder ->
                        SupplierOrderCard(supplierOrder)
                    }
                }

                if (uiState.error != null) {
                    Text(
                        uiState.error!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PharmaButton(
                        text = strings.allocationConfirmOrder,
                        onClick = { onConfirmed(order.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
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

@Composable
private fun SupplierOrderCard(supplierOrder: SupplierOrder) {
    var expanded by remember { mutableStateOf(true) }
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
                    Text(
                        "EGP ${formatDecimal(supplierOrder.subtotal, 2)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
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
                supplierOrder.lines.forEach { line -> DrugLineRow(line) }
            }
        }
    }
}

@Composable
private fun DrugLineRow(line: OrderDrugLine) {
    val strings = LocalStrings.current
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
                strings.allocationQtyPrice(line.qtyConfirmed ?: line.qtyRequested, formatDecimal(line.unitPrice, 2)),
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
