package com.pharmatrade.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.reminder.ReminderTime
import com.pharmatrade.core.ui.theme.*

// Seller agent's daily "upload your data" reminder times: hint, removable time chips, and an
// "Add reminder time" button opening the wheel picker. Used on Register and on Profile.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadRemindersEditor(
    times: List<ReminderTime>,
    onAdd: (ReminderTime) -> Unit,
    onRemove: (ReminderTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = strings.regUploadRemindersHint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        if (times.isEmpty()) {
            Text(text = strings.regNoRemindersYet, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                times.forEach { time ->
                    val label = time.format(strings.timeAm, strings.timePm)
                    InputChip(
                        selected = true,
                        onClick = { onRemove(time) },
                        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = { Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = strings.regRemoveReminderContentDescription(label), modifier = Modifier.size(16.dp)) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = PrimaryBlueContainer,
                            selectedLabelColor = PrimaryBlue,
                            selectedLeadingIconColor = PrimaryBlue,
                            selectedTrailingIconColor = PrimaryBlue
                        )
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { showPicker = true },
            enabled = times.size < ReminderTime.MAX_PER_DAY,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.AddAlarm, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.regAddReminderTime)
        }
    }

    if (showPicker) {
        ReminderTimePickerDialog(
            onConfirm = { time ->
                onAdd(time)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

// Hour / minute / AM-PM wheels, scrolled to pick a time — defaults to 12:00 PM.
@Composable
private fun ReminderTimePickerDialog(
    onConfirm: (ReminderTime) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val hours = remember { (1..12).map { it.toString() } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }
    val periods = listOf(strings.timeAm, strings.timePm)

    var hourIndex by remember { mutableStateOf(11) }   // "12"
    var minuteIndex by remember { mutableStateOf(0) }  // "00"
    var periodIndex by remember { mutableStateOf(1) }  // PM

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.regReminderDialogTitle) },
        text = {
            // Keep hour:minute AM/PM in reading order even when the app is in Arabic (RTL).
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // Band behind the centre row marking the current selection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryBlueContainer)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        WheelPicker(items = hours, initialIndex = hourIndex, onSelected = { hourIndex = it })
                        Text(":", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        WheelPicker(items = minutes, initialIndex = minuteIndex, onSelected = { minuteIndex = it })
                        WheelPicker(items = periods, initialIndex = periodIndex, onSelected = { periodIndex = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hour12 = hourIndex + 1
                val hour24 = (hour12 % 12) + if (periodIndex == 1) 12 else 0
                onConfirm(ReminderTime(hour = hour24, minute = minuteIndex))
            }) { Text(strings.regReminderAdd) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.commonCancel) }
        }
    )
}
