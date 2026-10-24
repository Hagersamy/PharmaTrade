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
import com.pharmatrade.core.common.i18n.LocalStrings
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
    val strings = LocalStrings.current

    LaunchedEffect(editingListingId) {
        editingListingId?.let { viewModel.loadForEdit(it) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray).imePadding()) {
        PharmaTopBar(
            title = if (editingListingId != null) strings.dfEditListingTitle else strings.dfAddDrugListingTitle,
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val selectedDrug = viewModel.selectedDrug
            FormSection(icon = Icons.Filled.Medication, title = strings.dfSelectDrug) {
                if (editingListingId == null) {
                    DrugSingleSelectDropdown(
                        drugs = uiState.availableDrugs,
                        selectedDrug = selectedDrug,
                        searchQuery = uiState.drugSearchQuery,
                        onSearchQueryChange = viewModel::onDrugSearchQueryChange,
                        isLoading = uiState.isLoadingDrugs || uiState.isSearchingDrugs,
                        hasError = uiState.drugsError != null,
                        onSelect = { viewModel.onDrugSelected(it.id) },
                        onAddNewDrug = viewModel::openAddDrugDialog,
                        onRetry = viewModel::loadDrugs
                    )
                }
                if (selectedDrug != null) {
                    SelectedDrugChip(selectedDrug)
                }
            }

            // No "customer pays" preview: the pharmacy price is calculated by the backend only.
            // Unit and expiry-date inputs were removed: POST/PUT supplier/inventory has no field
            // for either, so whatever the supplier entered was silently discarded.
            FormSection(icon = Icons.Filled.Sell, title = strings.dfPricingAvailability) {
                FormField(
                    value = uiState.priceInput,
                    onValueChange = viewModel::onPriceChange,
                    label = strings.dfPricePerUnit,
                    keyboardType = KeyboardType.Decimal,
                    leadingIcon = Icons.Filled.Payments,
                    suffix = "EGP"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        value = uiState.discountInput,
                        onValueChange = viewModel::onDiscountChange,
                        label = strings.dfDiscountPercentage,
                        keyboardType = KeyboardType.Decimal,
                        leadingIcon = Icons.Filled.Discount,
                        suffix = "%",
                        modifier = Modifier.weight(1f)
                    )
                    FormField(
                        value = uiState.quantityInput,
                        onValueChange = viewModel::onQuantityChange,
                        label = strings.dfQuantityAvailable,
                        keyboardType = KeyboardType.Number,
                        leadingIcon = Icons.Filled.Inventory2,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (uiState.error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRedContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.error!!, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
            }
        }

        // Save action pinned to the bottom so it stays reachable above the keyboard.
        Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
            PharmaButton(
                text = if (editingListingId != null) strings.dfUpdateListing else strings.dfAddListing,
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                isLoading = uiState.isLoading
            )
        }
    }

    if (uiState.showAddDrugDialog) {
        AddDrugDialog(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun FormSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            content()
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier.fillMaxWidth(),
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1) },
        leadingIcon = { Icon(leadingIcon, null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
        suffix = suffix?.let { { Text(it, color = TextSecondary) } },
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = DividerGray,
            focusedLabelColor = PrimaryBlue
        )
    )
}

@Composable
private fun SelectedDrugChip(drug: Drug) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SecondaryGreenContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = SecondaryGreenDark, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(drug.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
            if (drug.genericName.isNotBlank()) {
                Text(drug.genericName, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AddDrugDialog(uiState: DrugFormUiState, viewModel: DrugFormViewModel) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = { if (!uiState.isCreatingDrug) viewModel.dismissAddDrugDialog() },
        icon = { Icon(Icons.Filled.AddCircleOutline, null, tint = PrimaryBlue) },
        title = { Text(strings.dfAddNewDrugTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    strings.dfAddNewDrugDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = uiState.newDrugName,
                    onValueChange = viewModel::onNewDrugNameChange,
                    label = { Text(strings.dfDrugNameRequired) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = uiState.newDrugTradeName,
                    onValueChange = viewModel::onNewDrugTradeNameChange,
                    label = { Text(strings.dfTradeNameRequired) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = uiState.newDrugScientificName,
                    onValueChange = viewModel::onNewDrugScientificNameChange,
                    label = { Text(strings.dfScientificName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = uiState.newDrugManufacturer,
                    onValueChange = viewModel::onNewDrugManufacturerChange,
                    label = { Text(strings.dfManufacturer) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.newDrugDosageForm,
                        onValueChange = viewModel::onNewDrugDosageFormChange,
                        label = { Text(strings.dfDosageForm) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = uiState.newDrugStrength,
                        onValueChange = viewModel::onNewDrugStrengthChange,
                        label = { Text(strings.dfStrength) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(
                    value = uiState.newDrugBarcode,
                    onValueChange = viewModel::onNewDrugBarcodeChange,
                    label = { Text(strings.dfBarcodeOptional) },
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
                    Text(strings.dfAddDrugButton)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissAddDrugDialog, enabled = !uiState.isCreatingDrug) {
                Text(strings.dfCancel)
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
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChange(it)
                    expanded = true
                },
                label = { Text(strings.dfDrugFieldRequired) },
                placeholder = { Text(strings.dfTypeToSearchDrugs, color = TextSecondary) },
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
                            Text(strings.dfCantFindDrugAddIt, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
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
                Text(strings.dfCouldNotLoadDrugs, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                    Text(strings.dfRetry, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
