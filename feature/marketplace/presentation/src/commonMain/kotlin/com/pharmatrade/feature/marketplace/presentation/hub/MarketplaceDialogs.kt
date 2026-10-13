package com.pharmatrade.feature.marketplace.presentation.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.ui.theme.ErrorRed
import com.pharmatrade.core.ui.theme.PrimaryBlue
import com.pharmatrade.core.ui.theme.TextSecondary
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun CreateCollectionDialog(
    form: CreateCollectionForm,
    onEdit: (CreateCollectionForm.() -> CreateCollectionForm) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.mkNewCollection) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { v -> onEdit { copy(name = v) } },
                    label = { Text(strings.mkCollectionName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.description,
                    onValueChange = { v -> onEdit { copy(description = v) } },
                    label = { Text(strings.mkCollectionDescription) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.notes,
                    onValueChange = { v -> onEdit { copy(notes = v) } },
                    label = { Text(strings.mkCollectionNotes) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                form.error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = !form.isSaving) {
                if (form.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(strings.mkCreate)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !form.isSaving) { Text(strings.commonCancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddInventoryDialog(
    form: AddInventoryForm,
    onQueryChange: (String) -> Unit,
    onDrugSelected: (com.pharmatrade.core.common.model.Drug) -> Unit,
    onEdit: (AddInventoryForm.() -> AddInventoryForm) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.mkAddDrug) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = form.drugQuery,
                    onValueChange = onQueryChange,
                    label = { Text(strings.mkSearchDrug) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextSecondary) },
                    trailingIcon = {
                        if (form.isSearching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Results only while searching — picking one collapses the list into the field.
                if (form.selectedDrug == null && form.drugQuery.isNotBlank() && !form.isSearching) {
                    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                        if (form.drugResults.isEmpty()) {
                            Text(strings.mkNoDrugsFound, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        form.drugResults.forEach { drug ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onDrugSelected(drug) }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(drug.name, style = MaterialTheme.typography.bodyMedium)
                                val detail = listOf(drug.genericName, drug.strength, drug.dosageForm).filter { it.isNotBlank() }
                                if (detail.isNotEmpty()) {
                                    Text(detail.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                OutlinedTextField(
                    value = form.quantity,
                    onValueChange = { v -> onEdit { copy(quantity = v.filter { it.isDigit() }) } },
                    label = { Text(strings.mkQuantity) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.unitPrice,
                    onValueChange = { v -> onEdit { copy(unitPrice = v) } },
                    label = { Text(strings.mkUnitPrice) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                // Read-only field opening a date picker: a clickable overlay, since a disabled
                // text field would grey out and an enabled one would pop the keyboard.
                Box {
                    OutlinedTextField(
                        value = form.expiryDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.mkExpiryDate) },
                        trailingIcon = { Icon(Icons.Filled.CalendarMonth, null, tint = PrimaryBlue) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { showDatePicker = true })
                }
                OutlinedTextField(
                    value = form.batchNumber,
                    onValueChange = { v -> onEdit { copy(batchNumber = v) } },
                    label = { Text(strings.mkBatchNumber) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                form.error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = !form.isSaving) {
                if (form.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(strings.commonSave)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !form.isSaving) { Text(strings.commonCancel) } }
    )

    if (showDatePicker) {
        // The backend rejects past expiry dates, so only today onward is selectable.
        val todayStartUtc = remember {
            val now = Clock.System.now().toEpochMilliseconds()
            now - now % MILLIS_PER_DAY
        }
        val pickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis > todayStartUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        onEdit { copy(expiryDate = date.toString()) }
                    }
                    showDatePicker = false
                }) { Text(strings.commonConfirm) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.commonCancel) } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
