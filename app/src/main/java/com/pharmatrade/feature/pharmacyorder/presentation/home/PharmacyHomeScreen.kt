package com.pharmatrade.feature.pharmacyorder.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.ui.components.DiscountBadge
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.SectionHeader
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem

@Composable
fun PharmacyHomeScreen(
    viewModel: PharmacyHomeViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val filteredCatalog = remember(uiState.catalogItems, uiState.catalogFilter) {
        val query = uiState.catalogFilter.trim()
        if (query.isBlank()) uiState.catalogItems
        else uiState.catalogItems.filter {
            it.drugName.contains(query, ignoreCase = true) || it.supplierName.contains(query, ignoreCase = true)
        }
    }

    // Infinite scroll for the drug catalog section at the bottom of this single list.
    LaunchedEffect(listState, filteredCatalog.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisible ->
                if (lastVisible != null && lastVisible >= listState.layoutInfo.totalItemsCount - 5 && uiState.canLoadMoreCatalog) {
                    viewModel.loadNextCatalogPage()
                }
            }
    }

    if (uiState.pendingCatalogItem != null) {
        val pending = uiState.pendingCatalogItem!!
        CatalogQuantityDialog(
            item = pending,
            initialQuantity = uiState.cartQuantities[pending.id] ?: 1,
            onDismiss = viewModel::dismissCatalogDialog,
            onConfirm = viewModel::confirmAddCatalogItem
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(title = "All Suppliers' Drugs")
                        OutlinedTextField(
                            value = uiState.catalogFilter,
                            onValueChange = viewModel::onCatalogFilterChange,
                            placeholder = { Text("Search drug or supplier name") },
                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextSecondary) },
                            trailingIcon = {
                                if (uiState.catalogFilter.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onCatalogFilterChange("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = DividerGray,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            )
                        )
                    }
                }

                if (uiState.catalogActionError != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ErrorRedContainer)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(uiState.catalogActionError!!, style = MaterialTheme.typography.bodySmall, color = ErrorRed, modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::dismissCatalogActionError, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = ErrorRed, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                when {
                    uiState.isLoadingCatalogFirstPage && uiState.catalogItems.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    }
                    uiState.catalogError != null && uiState.catalogItems.isEmpty() -> item {
                        ErrorScreen(message = uiState.catalogError!!, onRetry = viewModel::retryCatalog, modifier = Modifier.height(220.dp))
                    }
                    filteredCatalog.isEmpty() -> item {
                        EmptyState(
                            title = if (uiState.catalogItems.isEmpty()) "No drugs available" else "No matches",
                            message = if (uiState.catalogItems.isEmpty()) "Check back later" else "Try a different search term",
                            icon = Icons.Filled.Medication,
                            modifier = Modifier.height(220.dp)
                        )
                    }
                    else -> {
                        itemsIndexed(
                            filteredCatalog,
                            // The catalog endpoint's exact response shape is unconfirmed, so `id`/`drugId`
                            // can't be trusted alone to be present or unique — fold in the list index too.
                            key = { index, catalogItem -> "${index}_${catalogItem.supplierId}_${catalogItem.drugId}_${catalogItem.id}" }
                        ) { _, catalogItem ->
                            CatalogDrugCard(
                                item = catalogItem,
                                addedQuantity = uiState.cartQuantities[catalogItem.id] ?: 0,
                                isProcessing = catalogItem.id in uiState.processingItemIds,
                                onQuickAdd = { viewModel.quickAddCatalogItem(catalogItem) },
                                onIncrease = { viewModel.incrementCartItem(catalogItem) },
                                onDecrease = { viewModel.decrementCartItem(catalogItem) },
                                onOpenDialog = { viewModel.onCatalogItemTapped(catalogItem) }
                            )
                        }
                        if (uiState.isLoadingCatalogMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── All-suppliers drug catalog card ───────────────────────────────────────────

@Composable
private fun CatalogDrugCard(
    item: SupplierCatalogItem,
    addedQuantity: Int,
    isProcessing: Boolean,
    onQuickAdd: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onOpenDialog: () -> Unit
) {
    val outOfStock = item.quantityAvailable <= 0
    val lowStock = !outOfStock && item.quantityAvailable < 10

    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !outOfStock, onClick = onOpenDialog)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (item.discountPct > 0) DiscountBadgeContainer else CardGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Medication,
                    contentDescription = null,
                    tint = if (item.discountPct > 0) DiscountBadge else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(item.drugName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                if (item.dosageForm.isNotBlank() || item.strength.isNotBlank()) {
                    Text(
                        listOf(item.dosageForm, item.strength).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(11.dp))
                    Text(item.supplierName, style = MaterialTheme.typography.labelSmall, color = PrimaryBlue, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StockChip(outOfStock = outOfStock, lowStock = lowStock, quantity = item.quantityAvailable)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (item.discountPct > 0) {
                    Text(
                        "EGP ${String.format("%.2f", item.unitPrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
                Text(
                    "EGP ${String.format("%.2f", item.effectivePrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (item.discountPct > 0) {
                    Spacer(Modifier.height(2.dp))
                    DiscountBadge(discountPercentage = item.discountPct)
                }
            }

            Spacer(Modifier.width(2.dp))

            if (addedQuantity > 0) {
                CartQuantityStepper(
                    quantity = addedQuantity,
                    isProcessing = isProcessing,
                    canIncrease = addedQuantity < item.quantityAvailable,
                    onIncrease = onIncrease,
                    onDecrease = onDecrease
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (!outOfStock) PrimaryBlue else DividerGray)
                        .clickable(enabled = !outOfStock && !isProcessing, onClick = onQuickAdd),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Filled.Add, contentDescription = "Add to cart", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// Compact +/- stepper for a catalog item already in the cart — lets the buyer adjust the
// quantity (e.g. added 3, tap "-" once to make it 2) without reopening the quantity dialog.
// "+" disables once the drug's available stock is reached. Not private: also reused by the
// Cart screen's per-item rows.
@Composable
fun CartQuantityStepper(
    quantity: Int,
    isProcessing: Boolean,
    canIncrease: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SecondaryGreenContainer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDecrease,
            enabled = !isProcessing,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", tint = SecondaryGreenDark, modifier = Modifier.size(16.dp))
        }
        Box(modifier = Modifier.widthIn(min = 18.dp), contentAlignment = Alignment.Center) {
            if (isProcessing) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = SecondaryGreenDark)
            } else {
                Text("$quantity", style = MaterialTheme.typography.labelMedium, color = SecondaryGreenDark, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(
            onClick = onIncrease,
            enabled = !isProcessing && canIncrease,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = if (canIncrease) SecondaryGreenDark else TextHint, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun StockChip(outOfStock: Boolean, lowStock: Boolean, quantity: Int) {
    val (label, color, bg) = when {
        outOfStock -> Triple("Out of stock", ErrorRed, ErrorRedContainer)
        lowStock -> Triple("Only $quantity left", WarningAmber, WarningAmberContainer)
        else -> Triple("$quantity in stock", SecondaryGreenDark, SecondaryGreenContainer)
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CatalogQuantityDialog(
    item: SupplierCatalogItem,
    initialQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val maxQuantity = item.quantityAvailable
    var quantity by remember(item.id) { mutableStateOf(initialQuantity.coerceIn(1, maxQuantity.coerceAtLeast(1))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.drugName, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "From ${item.supplierName} · EGP ${String.format("%.2f", item.effectivePrice)}/unit · $maxQuantity in stock",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        enabled = quantity > 1
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", tint = if (quantity > 1) PrimaryBlue else TextHint)
                    }
                    Text(
                        "$quantity",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.widthIn(min = 48.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(
                        onClick = { if (quantity < maxQuantity) quantity++ },
                        enabled = quantity < maxQuantity
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = if (quantity < maxQuantity) PrimaryBlue else TextHint)
                    }
                }
                if (quantity >= maxQuantity) {
                    Text(
                        "Max available reached",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(quantity) }, enabled = quantity in 1..maxQuantity) {
                Text("Add to cart")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
