package com.pharmatrade.feature.marketplace.presentation.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.LoadingScreen
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.PharmaOutlinedButton
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.components.SectionHeader
import com.pharmatrade.core.ui.components.rememberToast
import com.pharmatrade.core.ui.theme.BackgroundGray
import com.pharmatrade.core.ui.theme.ErrorRed
import com.pharmatrade.core.ui.theme.PrimaryBlue
import com.pharmatrade.core.ui.theme.SurfaceWhite
import com.pharmatrade.core.ui.theme.TextPrimary
import com.pharmatrade.core.ui.theme.TextSecondary
import com.pharmatrade.feature.marketplace.domain.model.CollectionStatus
import com.pharmatrade.feature.marketplace.domain.model.MarketCollection
import com.pharmatrade.feature.marketplace.presentation.components.CollectionItemRow
import com.pharmatrade.feature.marketplace.presentation.components.CollectionStatusChip
import com.pharmatrade.feature.marketplace.presentation.components.ConfirmDialog
import com.pharmatrade.feature.marketplace.presentation.components.money

@Composable
fun MarketCollectionScreen(
    viewModel: MarketCollectionViewModel,
    onNavigateBack: () -> Unit,
    onOrderPlaced: (orderId: String) -> Unit
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showToast = rememberToast(long = true)
    var confirmOrder by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            showToast(it)
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(state.placedOrderId) {
        state.placedOrderId?.let {
            viewModel.onOrderOpened()
            showToast(strings.mkOrderPlacedToast)
            onOrderPlaced(it)
        }
    }

    Scaffold(
        topBar = {
            PharmaTopBar(
                title = state.collection?.name?.takeIf { it.isNotBlank() } ?: strings.mkCollectionTitle,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            state.collection?.let { collection ->
                CollectionActions(
                    collection = collection,
                    mine = viewModel.mine,
                    isActing = state.isActing,
                    onOrder = { confirmOrder = true },
                    onEditItems = viewModel::openEditor,
                    onPublish = viewModel::publish,
                    onUnpublish = viewModel::deactivate
                )
            }
        },
        containerColor = BackgroundGray
    ) { padding ->
        val collection = state.collection
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(padding))
            collection == null -> ErrorScreen(
                message = state.error ?: strings.mkCollectionUnavailable,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding)
            )
            else -> CollectionBody(collection, showStatus = viewModel.mine, modifier = Modifier.padding(padding))
        }
    }

    if (confirmOrder) {
        ConfirmDialog(
            title = strings.mkOrderConfirmTitle,
            message = strings.mkOrderConfirmMessage(money(state.collection?.totalPrice ?: 0.0)),
            confirmLabel = strings.mkOrderCollection,
            onConfirm = viewModel::placeOrder,
            onDismiss = { confirmOrder = false }
        )
    }

    state.editor?.let { editor ->
        ItemsEditorDialog(
            editor = editor,
            onQuantityChange = viewModel::onEditorQuantityChange,
            onSave = viewModel::saveEditor,
            onDismiss = viewModel::dismissEditor
        )
    }
}

@Composable
private fun CollectionBody(collection: MarketCollection, showStatus: Boolean, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PharmaCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        collection.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (showStatus) CollectionStatusChip(collection.status)
                }
                if (!showStatus && collection.sellerName.isNotBlank()) {
                    Text(strings.mkFromSeller(collection.sellerName), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                if (collection.description.isNotBlank()) {
                    Text(collection.description, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Spacer(Modifier.height(4.dp))
                PriceLine(strings.mkTotal, strings.mkPrice(money(collection.totalPrice)), emphasized = true)
            }
        }

        if (collection.notes.isNotBlank()) {
            PharmaCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(strings.mkNotes, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(collection.notes, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
        }

        SectionHeader(strings.mkItems)
        PharmaCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (collection.items.isEmpty()) {
                    Text(
                        strings.mkCollectionNoItems,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                collection.items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    CollectionItemRow(item)
                }
            }
        }
    }
}

@Composable
private fun PriceLine(label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasized) PrimaryBlue else TextPrimary
        )
    }
}

@Composable
private fun CollectionActions(
    collection: MarketCollection,
    mine: Boolean,
    isActing: Boolean,
    onOrder: () -> Unit,
    onEditItems: () -> Unit,
    onPublish: () -> Unit,
    onUnpublish: () -> Unit
) {
    val strings = LocalStrings.current
    Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!mine) {
                if (collection.status == CollectionStatus.ACTIVE) {
                    PharmaButton(
                        text = strings.mkOrderCollection,
                        onClick = onOrder,
                        isLoading = isActing,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    InfoText(strings.mkCollectionUnavailable)
                }
                return@Column
            }
            when (collection.status) {
                CollectionStatus.DRAFT -> {
                    if (collection.items.isEmpty()) InfoText(strings.mkPublishHint)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PharmaOutlinedButton(
                            text = strings.mkEditItems,
                            onClick = onEditItems,
                            enabled = !isActing,
                            modifier = Modifier.weight(1f)
                        )
                        PharmaButton(
                            text = strings.mkPublish,
                            onClick = onPublish,
                            enabled = collection.items.isNotEmpty(),
                            isLoading = isActing,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                CollectionStatus.ACTIVE -> PharmaOutlinedButton(
                    text = strings.mkUnpublish,
                    onClick = onUnpublish,
                    enabled = !isActing,
                    modifier = Modifier.fillMaxWidth()
                )
                CollectionStatus.ORDERED -> InfoText(strings.mkCollectionLocked)
                CollectionStatus.COMPLETED -> InfoText(strings.mkCollectionSold)
                CollectionStatus.SUSPENDED -> InfoText(strings.mkCollectionSuspended)
                CollectionStatus.UNKNOWN -> Unit
            }
        }
    }
}

@Composable
private fun InfoText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ItemsEditorDialog(
    editor: ItemsEditorState,
    onQuantityChange: (inventoryId: String, text: String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.mkEditItemsTitle) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                when {
                    editor.isLoading -> Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                    editor.inventory.isEmpty() -> Text(editor.error ?: strings.mkNoStockForItems, color = TextSecondary)
                    else -> editor.inventory.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider()
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.drugName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    strings.mkStockLine(item.quantityAvailable, money(item.unitPrice)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = editor.quantities[item.id].orEmpty(),
                                onValueChange = { onQuantityChange(item.id, it) },
                                placeholder = { Text("0") },
                                supportingText = { Text(strings.mkMaxAvailable(item.quantityAvailable)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(96.dp)
                            )
                        }
                    }
                }
                if (editor.error != null && editor.inventory.isNotEmpty()) {
                    Text(editor.error, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !editor.isSaving && !editor.isLoading && editor.inventory.isNotEmpty()) {
                if (editor.isSaving) CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                else Text(strings.mkSaveItems)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !editor.isSaving) { Text(strings.commonCancel) } }
    )
}
