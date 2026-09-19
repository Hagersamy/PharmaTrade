package com.pharmatrade.feature.pharmacyorder.presentation.orderlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.i18n.Strings
import com.pharmatrade.core.common.util.formatBackendTimestamp
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.UnreadDot
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacyOrderSummary
import com.pharmatrade.feature.pharmacyorder.presentation.common.OrderStatusChip

@Composable
fun OrderListScreen(
    viewModel: OrderListViewModel,
    onOrderClick: (String) -> Unit,
    unreadOrderIds: Set<String> = emptySet()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        ScrollableTabRow(
            selectedTabIndex = OrderListTab.entries.indexOf(uiState.selectedTab),
            containerColor = SurfaceWhite,
            edgePadding = 12.dp
        ) {
            OrderListTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { viewModel.onTabSelected(tab) },
                    text = { Text(tab.localizedLabel(strings)) }
                )
            }
        }

        when {
            uiState.isLoading && uiState.orders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            uiState.error != null && uiState.orders.isEmpty() -> ErrorScreen(
                message = uiState.error!!,
                onRetry = viewModel::loadOrders,
                modifier = Modifier.fillMaxSize()
            )
            uiState.orders.isEmpty() -> EmptyState(
                title = strings.orderListEmptyTitle,
                message = strings.orderListEmptyMessage,
                icon = Icons.Filled.ReceiptLong,
                modifier = Modifier.fillMaxSize()
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.orders, key = { it.id }) { order ->
                    OrderRow(
                        order = order,
                        hasUnreadNotification = order.id in unreadOrderIds,
                        onClick = { onOrderClick(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: PharmacyOrderSummary, hasUnreadNotification: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasUnreadNotification) UnreadDot()
                    if (order.status.equals("partially_available", ignoreCase = true)) {
                        Icon(Icons.Filled.Warning, contentDescription = strings.orderNeedsAttention, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    }
                    Text(order.orderNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                OrderStatusChip(status = order.status)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatBackendTimestamp(order.date), style = MaterialTheme.typography.labelSmall, color = TextHint)
                Text(
                    "EGP ${formatDecimal(order.totalValue, 2)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun OrderListTab.localizedLabel(strings: Strings): String = when (this) {
    OrderListTab.ALL -> strings.tabAll
    OrderListTab.PENDING -> strings.statusPending
    OrderListTab.CONFIRMED -> strings.statusConfirmed
    OrderListTab.SHIPPED -> strings.statusShipped
    OrderListTab.DELIVERED -> strings.statusDelivered
}
