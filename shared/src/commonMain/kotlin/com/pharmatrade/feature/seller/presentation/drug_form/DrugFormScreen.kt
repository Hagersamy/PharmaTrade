package com.pharmatrade.feature.seller.presentation.drug_form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.*
import com.pharmatrade.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrugFormScreen(
    viewModel: DrugFormViewModel,
    editingListingId: String?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(editingListingId) {
        editingListingId?.let { viewModel.loadForEdit(it) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(
            title = if (editingListingId != null) "Edit Listing" else "Add Drug Listing",
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drug selector
            if (editingListingId == null) {
                SectionHeader("Select Drug")
                DrugSingleSelectDropdown(
                    drugs = uiState.availableDrugs,
                    selectedDrug = viewModel.selectedDrug,
                    searchQuery = uiState.drugSearchQuery,
                    onSearchQueryChange = viewModel::onDrugSearchQueryChange,
                    isLoading = uiState.isLoadingDrugs || uiState.isSearchingDrugs,
                    hasError = uiState.drugsError != null,
                    onSelect = { viewModel.onDrugSelected(it.id) },
                    onAddNewDrug = viewModel::openAddDrugDialog,
                    onRetry = viewModel::loadDrugs
                )
            } else {
                // Show drug name when editing
                val listing = viewModel.selectedDrug
                if (listing != null) {
                    PharmaCard {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Medication, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(listing.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(listing.genericName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            Divider(color = DividerGray)
            SectionHeader("Pricing & Availability")

            OutlinedTextField(
                value = uiState.priceInput,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Price per Unit (EGP)") },
                leadingIcon = { Icon(Icons.Filled.AttachMoney, null, tint = TextSecondary) },
                prefix = { Text("EGP ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = DividerGray)
            )

            OutlinedTextField(
                value = uiState.discountInput,
                onValueChange = viewModel::onDiscountChange,
                label = { Text("Discount Percentage") },
                leadingIcon = { Icon(Icons.Filled.Discount, null, tint = TextSecondary) },
                suffix = { Text("%") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = DividerGray)
            )

            // Price preview
            val price = uiState.priceInput.toDoubleOrNull()
            val discount = uiState.discountInput.toDoubleOrNull() ?: 0.0
            if (price != null && price > 0) {
                val finalPrice = price * (1.0 - discount / 100.0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SecondaryGreenContainer)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Customer pays:", style = MaterialTheme.typography.bodyMedium, color = SecondaryGreenDark)
                    Text(
                        "EGP ${formatDecimal(finalPrice, 2)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGreenDark
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.quantityInput,
                    onValueChange = viewModel::onQuantityChange,
                    label = { Text("Quantity Available") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = DividerGray)
                )

                var unitExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.unitInput,
                        onValueChange = {},
                        label = { Text("Unit") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = DividerGray)
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        uiState.unitOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.onUnitChange(option)
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.expiryInput,
                onValueChange = viewModel::onExpiryChange,
                label = { Text("Expiry Date (e.g. 2026-12-31)") },
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, tint = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = DividerGray)
            )

            if (uiState.error != null) {
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
                    Text(uiState.error!!, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
            }

            PharmaButton(
                text = if (editingListingId != null) "Update Listing" else "Add Listing",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                isLoading = uiState.isLoading
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    if (uiState.showAddDrugDialog) {
        AddDrugDialog(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun AddDrugDialog(uiState: DrugFormUiState, viewModel: DrugFormViewModel) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isCreatingDrug) viewModel.dismissAddDrugDialog() },
        icon = { Icon(Icons.Filled.AddCircleOutline, null, tint = PrimaryBlue) },
        title = { Text("Add New Drug", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "This registers a new drug in the catalogue so you can list it for sale.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = uiState.newDrugName,
                    onValueChange = viewModel::onNewDrugNameChange,
                    label = { Text("Drug name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = uiState.newDrugTradeName,
                    onValueChange = viewModel::onNewDrugTradeNameChange,
                    label = { Text("Trade name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = uiState.newDrugScientificName,
                    onValueChange = viewModel::onNewDrugScientificNameChange,
                    label = { Text("Scientific name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = uiState.newDrugManufacturer,
                    onValueChange = viewModel::onNewDrugManufacturerChange,
                    label = { Text("Manufacturer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.newDrugDosageForm,
                        onValueChange = viewModel::onNewDrugDosageFormChange,
                        label = { Text("Dosage form") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = uiState.newDrugStrength,
                        onValueChange = viewModel::onNewDrugStrengthChange,
                        label = { Text("Strength") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(
                    value = uiState.newDrugBarcode,
                    onValueChange = viewModel::onNewDrugBarcodeChange,
                    label = { Text("Barcode (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (uiState.addDrugError != null) {
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
                        Text(uiState.addDrugError, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::createNewDrug,
                enabled = !uiState.isCreatingDrug &&
                    uiState.newDrugName.isNotBlank() &&
                    uiState.newDrugTradeName.isNotBlank()
            ) {
                if (uiState.isCreatingDrug) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Add Drug")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissAddDrugDialog, enabled = !uiState.isCreatingDrug) {
                Text("Cancel")
            }
        }
    )
}

// ── Drug single-select dropdown ─────────────────────────────────────────────
// An editable search box (backed by a live, debounced GET /drugs?search= query in the
// ViewModel) with an anchored DropdownMenu of radio rows below it that closes on pick.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrugSingleSelectDropdown(
    drugs: List<Drug>,
    selectedDrug: Drug?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoading: Boolean,
    hasError: Boolean,
    onSelect: (Drug) -> Unit,
    onAddNewDrug: () -> Unit,
    onRetry: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChange(it)
                    expanded = true
                },
                label = { Text("Drug *") },
                placeholder = { Text("Type to search drugs…", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Medication, null, tint = TextSecondary) },
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                    } else {
                        val rotation by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f, label = "arrow"
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            null,
                            Modifier
                                .rotate(rotation)
                                .clickable { expanded = !expanded }
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusEvent { if (it.isFocused) expanded = true },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = if (expanded) PrimaryBlue else DividerGray,
                    focusedLabelColor = PrimaryBlue
                )
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 320.dp)
            ) {
                drugs.forEach { drug ->
                    val isSelected = drug.id == selectedDrug?.id
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                                )
                                Column {
                                    Text(
                                        drug.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) PrimaryBlue else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (drug.genericName.isNotBlank()) {
                                        Text(drug.genericName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }
                            }
                        },
                        onClick = {
                            onSelect(drug)
                            expanded = false   // close after selection
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    )
                }
                if (drugs.isNotEmpty()) {
                    HorizontalDivider(color = DividerGray, modifier = Modifier.padding(horizontal = 12.dp))
                }
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.AddCircleOutline, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Text("Can't find your drug? Add it", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    onClick = {
                        expanded = false
                        onAddNewDrug()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                )
            }
        }

        if (hasError) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                Text("Could not load drugs.", style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                    Text("Retry", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
