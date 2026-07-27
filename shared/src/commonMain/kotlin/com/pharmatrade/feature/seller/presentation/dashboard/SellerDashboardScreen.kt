package com.pharmatrade.feature.seller.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.util.formatBackendTimestamp
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.*
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.drugs.domain.model.InventoryItem

@Composable
fun SellerDashboardScreen(
    viewModel: SellerDashboardViewModel,
    onNavigateToAddListing: () -> Unit,
    onNavigateToEditListing: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToUploadInventory: () -> Unit = {},
    showTopBar: Boolean = true,
    onSearchTap: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onSnackbarShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = if (showTopBar) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddListing,
                containerColor = PrimaryBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Listing")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(BackgroundGray)) {
            if (showTopBar) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Seller Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.seller?.businessName ?: "Loading...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Row {
                            IconButton(onClick = onNavigateToUploadInventory) {
                                Icon(Icons.Filled.CloudUpload, null, tint = Color.White)
                            }
                            IconButton(onClick = viewModel::showMinOrderDialog) {
                                Icon(Icons.Filled.Settings, null, tint = Color.White)
                            }
                            IconButton(onClick = onLogout) {
                                Icon(Icons.Filled.Logout, null, tint = Color.White)
                            }
                        }
                    }

                    if (uiState.seller != null) {
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatChip(
                                label = "Listings",
                                value = "${uiState.totalItems}",
                                icon = Icons.Filled.Inventory,
                                modifier = Modifier.weight(1f)
                            )
                            StatChip(
                                label = "Min Order",
                                value = "EGP ${formatDecimal(uiState.seller!!.minimumOrderAmount, 0)}",
                                icon = Icons.Filled.ShoppingCart,
                                modifier = Modifier.weight(1f)
                            )
                            StatChip(
                                label = "Rating",
                                value = "${uiState.seller!!.rating}★",
                                icon = Icons.Filled.Star,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            } // end if (showTopBar)

            if (!showTopBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TappableSearchBar(
                        hint = "Search your listings...",
                        onClick = onSearchTap,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryBlue)
                            .clickable(onClick = onNavigateToUploadInventory),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Upload Inventory", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            when {
                uiState.isLoading -> LoadingScreen()
                uiState.error != null -> ErrorScreen(
                    message = uiState.error!!,
                    onRetry = viewModel::loadData
                )
                uiState.inventoryItems.isEmpty() -> EmptyState(
                    title = "No Listings Yet",
                    message = "Add listings manually or upload your inventory as an Excel sheet",
                    icon = Icons.Filled.Inventory,
                    action = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PharmaButton(text = "Add Listing", onClick = onNavigateToAddListing)
                            OutlinedButton(
                                onClick = onNavigateToUploadInventory,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue)
                            ) {
                                Icon(Icons.Filled.UploadFile, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Upload Excel Sheet", color = PrimaryBlue)
                            }
                        }
                    }
                )
                else -> {
                    val listState = rememberLazyListState()

                    // The backend paginates 30 items/page and a seller's inventory can run into
                    // the thousands, so more pages are pulled in as the user nears the bottom.
                    LaunchedEffect(listState, uiState.hasMorePages, uiState.isLoadingMore) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { lastVisibleIndex ->
                                if (lastVisibleIndex != null &&
                                    lastVisibleIndex >= uiState.inventoryItems.size - 5 &&
                                    uiState.hasMorePages &&
                                    !uiState.isLoadingMore
                                ) {
                                    viewModel.loadMoreInventory()
                                }
                            }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "My Drug Listings (${uiState.totalItems})",
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(uiState.inventoryItems, key = { it.id }) { inventoryItem ->
                            SellerInventoryListItemCard(
                                item = inventoryItem,
                                onEdit = { viewModel.openEditDialog(inventoryItem) }
                            )
                        }
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Min order dialog
    if (uiState.showMinOrderDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMinOrderDialog,
            icon = { Icon(Icons.Filled.ShoppingCart, null, tint = PrimaryBlue) },
            title = { Text("Minimum Order Amount", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Set the minimum purchase amount customers must meet when ordering from you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.newMinOrderInput,
                        onValueChange = viewModel::onMinOrderInputChange,
                        label = { Text("Amount (EGP)") },
                        prefix = { Text("EGP ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = viewModel::updateMinimumOrder) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissMinOrderDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit inventory item dialog
    if (uiState.editingItem != null) {
        EditInventoryItemDialog(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun EditInventoryItemDialog(uiState: SellerDashboardUiState, viewModel: SellerDashboardViewModel) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isSavingEdit) viewModel.dismissEditDialog() },
        icon = { Icon(Icons.Filled.Edit, null, tint = PrimaryBlue) },
        title = { Text("Edit Inventory Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.editDrugNameInput,
                    onValueChange = viewModel::onEditDrugNameChange,
                    label = { Text("Drug name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = uiState.editQuantityInput,
                    onValueChange = viewModel::onEditQuantityChange,
                    label = { Text("Quantity available") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = uiState.editPriceInput,
                    onValueChange = viewModel::onEditPriceChange,
                    label = { Text("Unit price (EGP)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = uiState.editDiscountInput,
                    onValueChange = viewModel::onEditDiscountChange,
                    label = { Text("Discount %") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (uiState.editError != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ErrorRedContainer)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.editError, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = viewModel::saveEdit, enabled = !uiState.isSavingEdit) {
                if (uiState.isSavingEdit) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissEditDialog, enabled = !uiState.isSavingEdit) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StatChip(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun SellerInventoryListItemCard(item: InventoryItem, onEdit: () -> Unit) {
    val isMatched = item.isCatalogMatched
    val catalogDrug = item.catalogDrug
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Medication,
                            null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            item.drugName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        /* Hidden for now — description under the drug name (what the seller
                        typed in the uploaded sheet, e.g. "Amoxil 500mg").
                        Text(
                            text = "As uploaded: ${item.drugNameRaw}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                        */
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    /* Hidden for now — catalog-matched/unmatched badge.
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isMatched) SecondaryGreenContainer else CardGray)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isMatched) "Matched" else "Unmatched",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMatched) SecondaryGreenDark else TextSecondary
                        )
                    }
                    */
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    }
                }
            }

            /* Hidden for now — full catalog record the item was matched against (id, generic
            name, trade name, dosage form and strength from the backend's nested "drug" object).
            if (catalogDrug != null) {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardGray)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "CATALOG MATCH · #${catalogDrug.id}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    if (catalogDrug.name.isNotBlank()) {
                        Text(catalogDrug.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                    Text(
                        buildString {
                            if (catalogDrug.tradeName.isNotBlank()) append(catalogDrug.tradeName)
                            if (catalogDrug.dosageForm.isNotBlank()) append(" · ${catalogDrug.dosageForm}")
                            if (catalogDrug.strength.isNotBlank()) append(" · ${catalogDrug.strength}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            */

            Spacer(Modifier.height(12.dp))
            Divider(color = DividerGray)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoItem(label = "Stock", value = "${item.quantityAvailable}", modifier = Modifier.weight(1f))
                InfoItem(label = "Discount", value = "${formatDecimal(item.discountPct, 0)}%", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoItem(label = "Public Price", value = "EGP ${formatDecimal(item.publicPrice, 2)}", modifier = Modifier.weight(1f))
                InfoItem(label = "Pharmacist Price", value = "EGP ${formatDecimal(item.pharmacistPrice, 2)}", modifier = Modifier.weight(1f))
            }

            if (item.discountPct > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SecondaryGreenContainer)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Effective price:", style = MaterialTheme.typography.labelSmall, color = SecondaryGreenDark)
                    Text(
                        "EGP ${formatDecimal(item.effectivePrice, 2)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGreenDark
                    )
                }
            }

            if (item.lastUpdated.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Schedule, null, tint = TextHint, modifier = Modifier.size(12.dp))
                    Text(
                        "Updated ${formatBackendTimestamp(item.lastUpdated)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
