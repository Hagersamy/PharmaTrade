package com.pharmatrade.feature.marketplace.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.util.formatBackendTimestamp
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.theme.ErrorRed
import com.pharmatrade.core.ui.theme.ErrorRedContainer
import com.pharmatrade.core.ui.theme.InTransitTeal
import com.pharmatrade.core.ui.theme.InTransitTealContainer
import com.pharmatrade.core.ui.theme.PrimaryBlue
import com.pharmatrade.core.ui.theme.PrimaryBlueContainer
import com.pharmatrade.core.ui.theme.SecondaryGreen
import com.pharmatrade.core.ui.theme.SecondaryGreenContainer
import com.pharmatrade.core.ui.theme.SecondaryGreenDark
import com.pharmatrade.core.ui.theme.TextPrimary
import com.pharmatrade.core.ui.theme.TextSecondary
import com.pharmatrade.core.ui.theme.WarningAmber
import com.pharmatrade.core.ui.theme.WarningAmberContainer
import com.pharmatrade.feature.marketplace.domain.model.CollectionStatus
import com.pharmatrade.feature.marketplace.domain.model.MarketCollection
import com.pharmatrade.feature.marketplace.domain.model.MarketCollectionItem
import com.pharmatrade.feature.marketplace.domain.model.MarketInventoryItem
import com.pharmatrade.feature.marketplace.domain.model.MarketOrder
import com.pharmatrade.feature.marketplace.domain.model.MarketOrderStatus

fun money(amount: Double): String = formatDecimal(amount, 2)

@Composable
private fun StatusPill(label: String, color: Color, background: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(20.dp), color = background, modifier = modifier) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CollectionStatusChip(status: CollectionStatus, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    when (status) {
        CollectionStatus.DRAFT -> StatusPill(strings.mkCollectionStatusDraft, WarningAmber, WarningAmberContainer, modifier)
        CollectionStatus.ACTIVE -> StatusPill(strings.mkCollectionStatusActive, SecondaryGreenDark, SecondaryGreenContainer, modifier)
        CollectionStatus.ORDERED -> StatusPill(strings.mkCollectionStatusOrdered, PrimaryBlue, PrimaryBlueContainer, modifier)
        CollectionStatus.COMPLETED -> StatusPill(strings.mkCollectionStatusCompleted, InTransitTeal, InTransitTealContainer, modifier)
        CollectionStatus.SUSPENDED -> StatusPill(strings.mkCollectionStatusSuspended, ErrorRed, ErrorRedContainer, modifier)
        CollectionStatus.UNKNOWN -> Unit
    }
}

@Composable
fun MarketOrderStatusChip(status: MarketOrderStatus, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    when (status) {
        MarketOrderStatus.PENDING_COLLECTOR -> StatusPill(strings.mkOrderStatusPendingCollector, WarningAmber, WarningAmberContainer, modifier)
        MarketOrderStatus.PENDING_SELLER -> StatusPill(strings.mkOrderStatusPendingSeller, PrimaryBlue, PrimaryBlueContainer, modifier)
        MarketOrderStatus.ACCEPTED -> StatusPill(strings.mkOrderStatusAccepted, SecondaryGreenDark, SecondaryGreenContainer, modifier)
        MarketOrderStatus.READY_FOR_PICKUP -> StatusPill(strings.mkOrderStatusReady, InTransitTeal, InTransitTealContainer, modifier)
        MarketOrderStatus.COMPLETED -> StatusPill(strings.mkOrderStatusCompleted, SecondaryGreen, SecondaryGreenContainer, modifier)
        MarketOrderStatus.REJECTED -> StatusPill(strings.mkOrderStatusRejected, ErrorRed, ErrorRedContainer, modifier)
        MarketOrderStatus.CANCELLED -> StatusPill(strings.mkOrderStatusCancelled, ErrorRed, ErrorRedContainer, modifier)
        MarketOrderStatus.UNKNOWN -> Unit
    }
}

@Composable
fun CollectionCard(
    collection: MarketCollection,
    showStatus: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    PharmaCard(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Inventory2, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showStatus) CollectionStatusChip(collection.status)
            }
            if (collection.sellerName.isNotBlank() && !showStatus) {
                Spacer(Modifier.height(4.dp))
                Text(strings.mkFromSeller(collection.sellerName), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (collection.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    collection.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.mkItemsCount(collection.itemsCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    strings.mkPrice(money(collection.totalPrice)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
fun MarketOrderCard(order: MarketOrder, asSeller: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    PharmaCard(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    order.orderNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MarketOrderStatusChip(order.status)
            }
            if (order.collectionName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(order.collectionName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            val counterpart = if (asSeller) order.buyerName else order.sellerName
            if (counterpart.isNotBlank()) {
                Text(
                    if (asSeller) strings.mkForBuyer(counterpart) else strings.mkFromSeller(counterpart),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatBackendTimestamp(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    strings.mkPrice(money(order.totalAmount)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
fun InventoryRow(item: MarketInventoryItem, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    PharmaCard(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Medication, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.drugName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    strings.mkStockLine(item.quantityAvailable, money(item.unitPrice)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                val meta = listOfNotNull(
                    item.expiryDate.takeIf { it.isNotBlank() }?.let { strings.mkExpires(it) },
                    item.batchNumber?.takeIf { it.isNotBlank() }?.let { strings.mkBatch(it) }
                ).joinToString(" · ")
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun CollectionItemRow(item: MarketCollectionItem, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.drugName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(
                listOfNotNull(
                    strings.mkItemLine(item.quantity, money(item.unitPrice)),
                    item.expiryDate?.takeIf { it.isNotBlank() }?.let { strings.mkExpires(it) }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(strings.mkPrice(money(item.lineTotal)), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(confirmLabel, color = if (destructive) ErrorRed else PrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.commonCancel) } }
    )
}
