package com.pharmatrade.feature.pharmacyorder.presentation.ordermode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderMode
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacySupplier

@Composable
fun OrderModeScreen(
    viewModel: OrderModeViewModel,
    onNavigateBack: () -> Unit,
    onOrderCreated: (orderId: String, supplierId: String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.createdOrderId) {
        uiState.createdOrderId?.let { orderId ->
            onOrderCreated(orderId, uiState.selectedSupplier?.id)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(title = "New Order", onNavigateBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "How would you like to order?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            OrderModeCard(
                title = "Best discount",
                description = "The engine picks the cheapest suppliers automatically. May split across multiple suppliers.",
                icon = Icons.Filled.LocalOffer,
                isSelected = uiState.selectedMode == OrderMode.BEST_DISCOUNT,
                onClick = { viewModel.onModeSelected(OrderMode.BEST_DISCOUNT) }
            )

            OrderModeCard(
                title = "Specific supplier",
                description = "Pick one trusted supplier. All items go to that supplier.",
                icon = Icons.Filled.Storefront,
                isSelected = uiState.selectedMode == OrderMode.SPECIFIC_SUPPLIER,
                onClick = { viewModel.onModeSelected(OrderMode.SPECIFIC_SUPPLIER) }
            )

            if (uiState.selectedMode == OrderMode.SPECIFIC_SUPPLIER) {
                SupplierSingleSelectDropdown(
                    suppliers = uiState.suppliers,
                    selectedSupplier = uiState.selectedSupplier,
                    isLoading = uiState.isLoadingSuppliers,
                    hasError = uiState.suppliersError != null,
                    onSelect = viewModel::onSupplierSelected
                )
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g. Urgent shortage list", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DividerGray,
                    focusedLabelColor = PrimaryBlue
                )
            )

            if (uiState.createError != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    Text(uiState.createError!!, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            PharmaButton(
                text = "Continue",
                onClick = viewModel::confirm,
                enabled = uiState.canConfirm,
                isLoading = uiState.isCreating,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OrderModeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PrimaryBlue else DividerGray,
                shape = RoundedCornerShape(14.dp)
            )
            .background(if (isSelected) PrimaryBlueContainer else SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) PrimaryBlue else TextSecondary, modifier = Modifier.size(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (isSelected) PrimaryBlue else TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierSingleSelectDropdown(
    suppliers: List<PharmacySupplier>,
    selectedSupplier: PharmacySupplier?,
    isLoading: Boolean,
    hasError: Boolean,
    onSelect: (PharmacySupplier) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = when {
                    isLoading -> "Loading suppliers..."
                    selectedSupplier != null -> selectedSupplier.name
                    else -> ""
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Supplier *") },
                placeholder = { Text("Select a supplier", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Storefront, null, tint = TextSecondary) },
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                    } else {
                        val rotation by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f, label = "arrow"
                        )
                        Icon(Icons.Filled.ArrowDropDown, null, Modifier.rotate(rotation))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = if (expanded) PrimaryBlue else DividerGray,
                    focusedLabelColor = PrimaryBlue,
                    disabledBorderColor = if (expanded) PrimaryBlue else DividerGray,
                    disabledLabelColor = if (expanded) PrimaryBlue else TextSecondary,
                    disabledTextColor = TextPrimary,
                    disabledLeadingIconColor = TextSecondary,
                    disabledTrailingIconColor = TextSecondary
                ),
                enabled = false
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = !isLoading && suppliers.isNotEmpty()) { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 320.dp)
            ) {
                suppliers.forEach { supplier ->
                    val isSelected = supplier.id == selectedSupplier?.id
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue))
                                Column {
                                    Text(
                                        supplier.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) PrimaryBlue else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        buildString {
                                            append("Min order EGP ${formatDecimal(supplier.minOrderValue, 2)}")
                                            append(" · ${supplier.inventoryCount} items")
                                            supplier.zones.firstOrNull()?.let { append(" · ${it.name}") }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        },
                        onClick = { onSelect(supplier); expanded = false },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (hasError) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                Text("Could not load suppliers.", style = MaterialTheme.typography.bodySmall, color = ErrorRed)
            }
        }
    }
}
