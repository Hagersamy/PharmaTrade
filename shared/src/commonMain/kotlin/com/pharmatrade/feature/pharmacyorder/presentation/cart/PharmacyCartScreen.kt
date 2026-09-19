package com.pharmatrade.feature.pharmacyorder.presentation.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.SectionHeader
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem
import com.pharmatrade.feature.pharmacyorder.presentation.home.CartQuantityStepper
import com.pharmatrade.feature.pharmacyorder.presentation.home.PharmacyHomeViewModel
import com.pharmatrade.feature.pharmacyorder.presentation.home.SupplierCartOrder

// Embedded as the "Cart" bottom-bar tab on Home (like Orders/Profile), not a separate nav
// destination — so it reuses PharmacyHomeViewModel directly (same instance already alive on
// Home) rather than a dedicated ViewModel. Every action here (stepper +/-, remove order) is
// already a method on it, and the cart's source of truth (cartQuantities / catalogItems /
// supplierCarts) already lives there.
@Composable
fun PharmacyCartScreen(
    viewModel: PharmacyHomeViewModel,
    onCheckoutAll: (orders: List<Pair<String, String>>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    val itemsBySupplier = remember(uiState.catalogItems, uiState.cartQuantities) {
        val itemsById = uiState.catalogItems.associateBy { it.id }
        uiState.cartQuantities
            .mapNotNull { (catalogItemId, quantity) -> itemsById[catalogItemId]?.let { Triple(it, quantity, catalogItemId) } }
            .filter { (_, quantity, _) -> quantity > 0 }
            .groupBy { (item, _, _) -> item.supplierId }
    }
    val carts = uiState.supplierCarts.values.sortedBy { it.supplierName }
    val grandTotal = carts.sumOf { it.subtotal }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        Box(modifier = Modifier.padding(16.dp)) {
            SectionHeader(title = strings.pharmacyCartTitle)
        }

        if (carts.isEmpty()) {
            EmptyState(
                title = strings.pharmacyCartEmptyTitle,
                message = strings.pharmacyCartEmptyMessage,
                icon = Icons.Filled.ShoppingCart,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(carts, key = { it.supplierId }) { cart ->
                    SupplierCartCard(
                        cart = cart,
                        items = itemsBySupplier[cart.supplierId].orEmpty(),
                        processingItemIds = uiState.processingItemIds,
                        onIncrease = { item -> viewModel.incrementCartItem(item) },
                        onDecrease = { item -> viewModel.decrementCartItem(item) },
                        onRemoveOrder = { viewModel.removeSupplierOrder(cart.supplierId) }
                    )
                }
            }

            Surface(shadowElevation = 8.dp, color = SurfaceWhite) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                strings.pharmacyCartItemsOrdersSummary(uiState.totalCartItems, carts.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                "EGP ${formatDecimal(grandTotal, 2)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                    PharmaButton(
                        text = strings.pharmacyCartCheckoutAll(carts.size),
                        onClick = { onCheckoutAll(carts.map { it.orderId to it.supplierId }) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierCartCard(
    cart: SupplierCartOrder,
    items: List<Triple<SupplierCatalogItem, Int, String>>,
    processingItemIds: Set<String>,
    onIncrease: (SupplierCatalogItem) -> Unit,
    onDecrease: (SupplierCatalogItem) -> Unit,
    onRemoveOrder: () -> Unit
) {
    val strings = LocalStrings.current
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(PrimaryBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                    }
                    Text(cart.supplierName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                TextButton(onClick = onRemoveOrder) {
                    Text(strings.pharmacyCartRemoveOrder, color = ErrorRed)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = DividerGray)
            Spacer(Modifier.height(8.dp))

            items.forEach { (item, quantity, _) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.drugName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1)
                        Text(
                            strings.pharmacyCartLineTotal(formatDecimal(item.effectivePrice, 2), quantity, formatDecimal(item.effectivePrice * quantity, 2)),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    CartQuantityStepper(
                        quantity = quantity,
                        isProcessing = item.id in processingItemIds,
                        canIncrease = quantity < item.quantityAvailable,
                        onIncrease = { onIncrease(item) },
                        onDecrease = { onDecrease(item) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = DividerGray)
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.commonSubtotal, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    "EGP ${formatDecimal(cart.subtotal, 2)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}
