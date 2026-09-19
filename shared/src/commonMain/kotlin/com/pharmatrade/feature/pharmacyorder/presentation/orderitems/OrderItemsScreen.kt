package com.pharmatrade.feature.pharmacyorder.presentation.orderitems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.io.rememberFilePickerLauncher
import com.pharmatrade.core.ui.components.DiscountBadge
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.domain.model.DraftOrderItem
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacySupplier
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierInventoryItem

@Composable
fun OrderItemsScreen(
    viewModel: OrderItemsViewModel,
    onNavigateBack: () -> Unit,
    onReviewAndAllocate: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current

    val launchFilePicker = rememberFilePickerLauncher(
        mimeTypes = listOf(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "text/comma-separated-values"
        )
    ) { uri, displayName -> viewModel.uploadFile(uri, displayName) }

    LaunchedEffect(uiState.uploadResult) {
        uiState.uploadResult?.let { result ->
            val message = buildString {
                append(strings.oiItemsAddedMessage(result.addedCount))
                if (result.skippedCount > 0) {
                    append(strings.oiItemsSkippedSuffix(result.skippedCount))
                }
            }
            snackbarHostState.showSnackbar(message)
            viewModel.dismissUploadResult()
        }
    }

    LaunchedEffect(uiState.uploadError) {
        uiState.uploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissUploadError()
        }
    }

    LaunchedEffect(uiState.addItemError) {
        uiState.addItemError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    if (uiState.pendingItem != null) {
        QuantityDialog(
            item = uiState.pendingItem!!,
            isSubmitting = uiState.isAddingItem,
            errorMessage = uiState.addItemError,
            onDismiss = viewModel::dismissQuantityDialog,
            onConfirm = viewModel::confirmAddItem
        )
    }

    val quantityByDrugId = remember(uiState.items) {
        uiState.items.groupBy { it.drugId }.mapValues { (_, v) -> v.sumOf { it.quantity } }
    }
    val priceByDrugId = remember(uiState.supplierInventory) {
        uiState.supplierInventory.associateBy { it.drugId }
    }
    val orderSubtotal = remember(uiState.items, priceByDrugId) {
        uiState.items.sumOf { (priceByDrugId[it.drugId]?.effectivePrice ?: 0.0) * it.quantity }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(BackgroundGray)) {
            PharmaTopBar(
                title = if (viewModel.isSpecificSupplier) (uiState.supplier?.name?.takeIf { it.isNotBlank() } ?: strings.oiSupplierInventoryFallback) else strings.oiAddItemsTitle,
                onNavigateBack = onNavigateBack
            )

            if (viewModel.isSpecificSupplier) {
                SpecificSupplierContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    quantityByDrugId = quantityByDrugId,
                    orderSubtotal = orderSubtotal,
                    onUploadClick = launchFilePicker,
                    onReviewAndAllocate = onReviewAndAllocate
                )
            } else {
                BestDiscountContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onUploadClick = launchFilePicker,
                    onReviewAndAllocate = onReviewAndAllocate
                )
            }
        }
    }
}

// ── Specific-supplier mode: browse & order directly from this supplier's stock ─

@Composable
private fun ColumnScope.SpecificSupplierContent(
    uiState: OrderItemsUiState,
    viewModel: OrderItemsViewModel,
    quantityByDrugId: Map<String, Int>,
    orderSubtotal: Double,
    onUploadClick: () -> Unit,
    onReviewAndAllocate: () -> Unit
) {
    val strings = LocalStrings.current
    val filtered = remember(uiState.supplierInventory, uiState.supplierInventoryFilter) {
        val query = uiState.supplierInventoryFilter.trim()
        if (query.isBlank()) uiState.supplierInventory
        else uiState.supplierInventory.filter { it.drugName.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when {
            uiState.isLoadingSupplierInventory && uiState.supplierInventory.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            uiState.supplierInventoryError != null && uiState.supplierInventory.isEmpty() ->
                ErrorScreen(message = uiState.supplierInventoryError!!, onRetry = {}, modifier = Modifier.fillMaxSize())
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.supplier?.let { supplier ->
                    item { SupplierContextCard(supplier = supplier, itemCount = uiState.supplierInventory.size, orderSubtotal = orderSubtotal) }
                }

                item {
                    SearchAndUploadRow(
                        query = uiState.supplierInventoryFilter,
                        onQueryChange = viewModel::onSupplierInventoryFilterChange,
                        placeholder = uiState.supplier?.name?.takeIf { it.isNotBlank() }?.let { strings.oiFilterSupplierDrugs(it) } ?: strings.oiFilterDrugsGeneric,
                        isUploading = uiState.isUploading,
                        onUploadClick = onUploadClick
                    )
                }

                if (uiState.supplierInventory.isNotEmpty()) {
                    item {
                        Text(
                            strings.oiDrugsCountOfTotal(filtered.size, uiState.supplierInventory.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyState(
                            title = if (uiState.supplierInventory.isEmpty()) strings.oiNoInventoryTitle else strings.pharmacyNoMatches,
                            message = if (uiState.supplierInventory.isEmpty()) strings.oiNoInventoryMessage else strings.pharmacyTryDifferentSearch,
                            icon = Icons.Filled.Storefront,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { invItem ->
                        SupplierDrugCard(
                            item = invItem,
                            addedQuantity = quantityByDrugId[invItem.drugId] ?: 0,
                            isQuickAdding = invItem.id in uiState.quickAddingItemIds,
                            onQuickAdd = { viewModel.quickAddSupplierItem(invItem) },
                            onOpenDialog = { viewModel.onSupplierItemSelected(invItem) }
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    OrderSummaryFooter(
        itemCount = uiState.items.size,
        subtotal = orderSubtotal,
        minOrderValue = uiState.supplier?.minOrderValue ?: 0.0,
        onReviewAndAllocate = onReviewAndAllocate,
        canReview = uiState.items.isNotEmpty()
    )
}

@Composable
private fun SupplierContextCard(supplier: PharmacySupplier, itemCount: Int, orderSubtotal: Double) {
    val strings = LocalStrings.current
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(PrimaryBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(supplier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.oiDrugsAvailableCount(itemCount), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        supplier.zones.firstOrNull()?.let { zone ->
                            Text("·", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                            Text(zone.name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }

            if (supplier.minOrderValue > 0) {
                Spacer(Modifier.height(14.dp))
                val progress = (orderSubtotal / supplier.minOrderValue).toFloat().coerceIn(0f, 1f)
                val metMinimum = orderSubtotal >= supplier.minOrderValue
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        strings.oiMinimumOrderLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        strings.oiMinOrderProgress(formatDecimal(orderSubtotal, 2), formatDecimal(supplier.minOrderValue, 2)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (metMinimum) SecondaryGreenDark else TextSecondary
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (metMinimum) SecondaryGreen else PrimaryBlue,
                    trackColor = CardGray
                )
            }
        }
    }
}

@Composable
private fun SearchAndUploadRow(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    isUploading: Boolean,
    onUploadClick: () -> Unit
) {
    val strings = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder, maxLines = 1, style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = strings.commonClear, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = DividerGray,
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite
            )
        )
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isUploading) CardGray else PrimaryBlueContainer)
                .clickable(enabled = !isUploading, onClick = onUploadClick),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
            } else {
                Icon(Icons.Filled.UploadFile, contentDescription = strings.oiUploadExcelCsvContentDescription, tint = PrimaryBlue)
            }
        }
    }
}

@Composable
private fun SupplierDrugCard(
    item: SupplierInventoryItem,
    addedQuantity: Int,
    isQuickAdding: Boolean,
    onQuickAdd: () -> Unit,
    onOpenDialog: () -> Unit
) {
    val outOfStock = item.quantityAvailable <= 0
    val lowStock = !outOfStock && item.quantityAvailable < 10
    val strings = LocalStrings.current

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
                Text(
                    item.drugName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1
                )
                if (item.dosageForm.isNotBlank() || item.strength.isNotBlank()) {
                    Text(
                        listOf(item.dosageForm, item.strength).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StockChip(outOfStock = outOfStock, lowStock = lowStock, quantity = item.quantityAvailable)
                    if (addedQuantity > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SecondaryGreenContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = SecondaryGreenDark, modifier = Modifier.size(11.dp))
                            Text(
                                strings.oiAddedToOrder(addedQuantity),
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGreenDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (item.discountPct > 0) {
                    Text(
                        "EGP ${formatDecimal(item.unitPrice, 2)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
                Text(
                    "EGP ${formatDecimal(item.effectivePrice, 2)}",
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

            QuickAddButton(enabled = !outOfStock, isLoading = isQuickAdding, onClick = onQuickAdd)
        }
    }
}

@Composable
private fun StockChip(outOfStock: Boolean, lowStock: Boolean, quantity: Int) {
    val strings = LocalStrings.current
    val (label, color, bg) = when {
        outOfStock -> Triple(strings.pharmacyOutOfStock, ErrorRed, ErrorRedContainer)
        lowStock -> Triple(strings.pharmacyOnlyLeft(quantity), WarningAmber, WarningAmberContainer)
        else -> Triple(strings.pharmacyInStock(quantity), SecondaryGreenDark, SecondaryGreenContainer)
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
private fun QuickAddButton(enabled: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) PrimaryBlue else DividerGray)
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
            Icon(Icons.Filled.Add, contentDescription = strings.oiQuickAddContentDescription, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun OrderSummaryFooter(
    itemCount: Int,
    subtotal: Double,
    minOrderValue: Double,
    onReviewAndAllocate: () -> Unit,
    canReview: Boolean
) {
    val strings = LocalStrings.current
    Surface(shadowElevation = 8.dp, color = SurfaceWhite) {
        Column(modifier = Modifier.padding(16.dp)) {
            AnimatedVisibility(visible = itemCount > 0, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.oiItemsAddedMessage(itemCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        "EGP ${formatDecimal(subtotal, 2)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
            PharmaButton(
                text = strings.oiReviewAndAllocate,
                onClick = onReviewAndAllocate,
                enabled = canReview,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Best-discount mode: generic catalog search (allocation engine picks suppliers later) ─

@Composable
private fun ColumnScope.BestDiscountContent(
    uiState: OrderItemsUiState,
    viewModel: OrderItemsViewModel,
    onUploadClick: () -> Unit,
    onReviewAndAllocate: () -> Unit
) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            label = { Text(strings.oiSearchDrugsLabel) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextSecondary) },
            trailingIcon = {
                if (uiState.isSearching) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = DividerGray,
                focusedLabelColor = PrimaryBlue
            )
        )

        if (uiState.searchResults.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PharmaCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    uiState.searchResults.forEach { drug ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onDrugSelected(drug) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text(drug.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                if (drug.strength.isNotBlank() || drug.dosageForm.isNotBlank()) {
                                    Text(
                                        listOf(drug.dosageForm, drug.strength).filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = DividerGray)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onUploadClick,
            enabled = !uiState.isUploading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isUploading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                Spacer(Modifier.width(8.dp))
                Text(strings.oiUploading)
            } else {
                Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.oiUploadExcelCsv)
            }
        }
    }

    HorizontalDivider(color = DividerGray)

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when {
            uiState.isLoadingItems && uiState.items.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            uiState.items.isEmpty() -> EmptyState(
                title = strings.oiNoItemsYetTitle,
                message = strings.oiNoItemsYetMessage,
                icon = Icons.Filled.Search
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        strings.oiInThisOrder(uiState.items.size),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(uiState.items, key = { it.id }) { item ->
                    ItemRow(item = item, onDelete = { viewModel.removeItem(item.id) })
                }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        PharmaButton(
            text = strings.oiReviewAndAllocate,
            onClick = onReviewAndAllocate,
            enabled = uiState.items.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ItemRow(item: DraftOrderItem, onDelete: () -> Unit) {
    val strings = LocalStrings.current
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.drugName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(strings.oiQtyLabel(item.quantity), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = strings.oiRemoveItemContentDescription(item.drugName), tint = ErrorRed)
            }
        }
    }
}

@Composable
private fun QuantityDialog(
    item: PendingAddItem,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantityText by remember(item.drugId) { mutableStateOf("1") }
    val quantity = quantityText.toIntOrNull()
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(item.displayName, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (item.priceInfo != null) {
                    Text(item.priceInfo, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.length <= 6) quantityText = it.filter(Char::isDigit) },
                    label = { Text(strings.oiQuantityLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMessage, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { quantity?.let(onConfirm) },
                enabled = !isSubmitting && quantity != null && quantity > 0
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(strings.catalogAdd)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(strings.commonCancel)
            }
        }
    )
}
