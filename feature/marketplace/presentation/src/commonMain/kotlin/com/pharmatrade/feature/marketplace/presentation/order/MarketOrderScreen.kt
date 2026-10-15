package com.pharmatrade.feature.marketplace.presentation.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.util.formatBackendTimestamp
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.LoadingScreen
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.PharmaOutlinedButton
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.components.SectionHeader
import com.pharmatrade.core.ui.components.rememberToast
import com.pharmatrade.core.ui.theme.BackgroundGray
import com.pharmatrade.core.ui.theme.DividerGray
import com.pharmatrade.core.ui.theme.ErrorRed
import com.pharmatrade.core.ui.theme.PrimaryBlue
import com.pharmatrade.core.ui.theme.SecondaryGreen
import com.pharmatrade.core.ui.theme.SurfaceWhite
import com.pharmatrade.core.ui.theme.TextPrimary
import com.pharmatrade.core.ui.theme.TextSecondary
import com.pharmatrade.feature.marketplace.domain.model.MarketOrder
import com.pharmatrade.feature.marketplace.domain.model.MarketOrderStatus
import com.pharmatrade.feature.marketplace.domain.model.SellerOrderAction
import com.pharmatrade.feature.marketplace.presentation.components.CollectionItemRow
import com.pharmatrade.feature.marketplace.presentation.components.ConfirmDialog
import com.pharmatrade.feature.marketplace.presentation.components.MarketOrderStatusChip
import com.pharmatrade.feature.marketplace.presentation.components.money

// Which destructive action is waiting for confirmation.
private enum class PendingConfirm { BUYER_CANCEL, SELLER_CANCEL, REJECT, COMPLETE }

@Composable
fun MarketOrderScreen(viewModel: MarketOrderViewModel, onNavigateBack: () -> Unit) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showToast = rememberToast(long = true)
    var pendingConfirm by rememberSaveable { mutableStateOf<PendingConfirm?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            showToast(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = { PharmaTopBar(title = state.order?.orderNumber ?: strings.mkOrderTitle, onNavigateBack = onNavigateBack) },
        bottomBar = {
            state.order?.let { order ->
                OrderActions(
                    order = order,
                    asSeller = viewModel.asSeller,
                    isActing = state.isActing,
                    onAccept = { viewModel.sellerAction(SellerOrderAction.ACCEPT) },
                    onReady = { viewModel.sellerAction(SellerOrderAction.READY) },
                    onConfirm = { pendingConfirm = it }
                )
            }
        },
        containerColor = BackgroundGray
    ) { padding ->
        val order = state.order
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(padding))
            order == null -> ErrorScreen(
                message = state.error ?: strings.errorGeneric,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding)
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OrderHeader(order, viewModel.asSeller)
                if (!viewModel.asSeller && order.status == MarketOrderStatus.PENDING_COLLECTOR) {
                    CollectorForm(
                        name = state.collectorName,
                        phone = state.collectorPhone,
                        error = state.collectorError,
                        isSaving = state.isActing,
                        onNameChange = viewModel::onCollectorNameChange,
                        onPhoneChange = viewModel::onCollectorPhoneChange,
                        onSubmit = viewModel::submitCollector
                    )
                }
                // Once the seller accepts, each side gets the other's contact details, kept while
                // the order is ready and after it's completed. The buyer's collector needs the
                // seller's address to pick the order up.
                val accepted = order.status in ACCEPTED_OR_LATER
                if (accepted && viewModel.asSeller) {
                    ContactCard(
                        title = strings.mkBuyerDetails,
                        name = order.buyerName,
                        phone = order.buyerPhone,
                        address = order.buyerAddress,
                        addressLabel = strings.mkBuyerAddress
                    )
                } else if (accepted) {
                    ContactCard(
                        title = strings.mkSellerDetails,
                        name = order.sellerName,
                        phone = order.sellerPhone,
                        address = order.collectorAddress ?: order.sellerAddress,
                        addressLabel = strings.mkCollectorAddress
                    )
                }
                val pickupAddress = order.collectorAddress?.takeIf { accepted && viewModel.asSeller }
                if (order.collectorName != null || pickupAddress != null) CollectorCard(order, pickupAddress)
                AmountsCard(order, viewModel.asSeller)
                if (order.items.isNotEmpty()) {
                    SectionHeader(strings.mkItems)
                    PharmaCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            order.items.forEachIndexed { index, item ->
                                if (index > 0) HorizontalDivider()
                                CollectionItemRow(item)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingConfirm?.let { confirm ->
        val dismiss = { pendingConfirm = null }
        when (confirm) {
            PendingConfirm.BUYER_CANCEL -> ConfirmDialog(
                title = strings.mkCancelOrder,
                message = strings.mkCancelOrderConfirm,
                confirmLabel = strings.mkCancelOrder,
                onConfirm = viewModel::cancelAsBuyer,
                onDismiss = dismiss,
                destructive = true
            )
            PendingConfirm.SELLER_CANCEL -> ConfirmDialog(
                title = strings.mkCancelOrder,
                message = strings.mkCancelOrderConfirm,
                confirmLabel = strings.mkCancelOrder,
                onConfirm = { viewModel.sellerAction(SellerOrderAction.CANCEL) },
                onDismiss = dismiss,
                destructive = true
            )
            PendingConfirm.REJECT -> ConfirmDialog(
                title = strings.mkReject,
                message = strings.mkRejectConfirm,
                confirmLabel = strings.mkReject,
                onConfirm = { viewModel.sellerAction(SellerOrderAction.REJECT) },
                onDismiss = dismiss,
                destructive = true
            )
            PendingConfirm.COMPLETE -> ConfirmDialog(
                title = strings.mkComplete,
                message = strings.mkCompleteConfirm,
                confirmLabel = strings.commonConfirm,
                onConfirm = { viewModel.sellerAction(SellerOrderAction.COMPLETE) },
                onDismiss = dismiss
            )
        }
    }
}

@Composable
private fun OrderHeader(order: MarketOrder, asSeller: Boolean) {
    val strings = LocalStrings.current
    PharmaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    order.collectionName.ifBlank { order.orderNumber },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MarketOrderStatusChip(order.status)
            }
            val counterpart = if (asSeller) order.buyerName else order.sellerName
            if (counterpart.isNotBlank()) {
                Text(
                    "${if (asSeller) strings.mkBuyer else strings.mkSeller}: $counterpart",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            if (order.createdAt.isNotBlank()) {
                Text(formatBackendTimestamp(order.createdAt), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            if (order.status == MarketOrderStatus.REJECTED || order.status == MarketOrderStatus.CANCELLED) {
                Text(
                    if (order.status == MarketOrderStatus.REJECTED) strings.mkOrderRejectedMsg else strings.mkOrderCancelledMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                StatusSteps(order.status)
            }
        }
    }
}

// placed → collector named → accepted → ready → completed
@Composable
private fun StatusSteps(status: MarketOrderStatus) {
    val strings = LocalStrings.current
    val labels = listOf(strings.mkStepPlaced, strings.mkStepCollector, strings.mkStepAccepted, strings.mkStepReady, strings.mkStepCompleted)
    val reached = when (status) {
        MarketOrderStatus.PENDING_COLLECTOR -> 1
        MarketOrderStatus.PENDING_SELLER -> 2
        MarketOrderStatus.ACCEPTED -> 3
        MarketOrderStatus.READY_FOR_PICKUP -> 4
        MarketOrderStatus.COMPLETED -> 5
        else -> 0
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        labels.forEachIndexed { index, label ->
            val done = index < reached
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                    // Connector halves on either side of the dot.
                    Row(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.weight(1f).height(2.dp)
                                .background(if (index == 0) Color.Transparent else if (done) SecondaryGreen else DividerGray)
                        )
                        Box(
                            Modifier.weight(1f).height(2.dp)
                                .background(if (index == labels.lastIndex) Color.Transparent else if (index + 1 < reached) SecondaryGreen else DividerGray)
                        )
                    }
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(if (done) SecondaryGreen else DividerGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) Icon(Icons.Filled.Check, null, tint = SurfaceWhite, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (done) TextPrimary else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CollectorForm(
    name: String,
    phone: String,
    error: String?,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val strings = LocalStrings.current
    PharmaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(strings.mkCollector, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(strings.mkCollectorHint, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(strings.mkCollectorName) },
                leadingIcon = { Icon(Icons.Filled.Person, null, tint = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text(strings.mkCollectorPhone) },
                leadingIcon = { Icon(Icons.Filled.Phone, null, tint = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }
            PharmaButton(
                text = strings.mkConfirmCollector,
                onClick = onSubmit,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val ACCEPTED_OR_LATER = setOf(
    MarketOrderStatus.ACCEPTED,
    MarketOrderStatus.READY_FOR_PICKUP,
    MarketOrderStatus.COMPLETED
)

@Composable
private fun ContactCard(title: String, name: String, phone: String?, address: String?, addressLabel: String) {
    if (name.isBlank() && phone == null && address == null) return
    PharmaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            if (name.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storefront, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
                }
            }
            phone?.let { phone ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Phone, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(phone, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                }
            }
            address?.let { address ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.LocationOn, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(addressLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(address, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectorCard(order: MarketOrder, address: String?) {
    val strings = LocalStrings.current
    PharmaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(strings.mkCollector, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            order.collectorName?.let { name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                }
            }
            order.collectorPhone?.let { phone ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Phone, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(phone, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                }
            }
            address?.let {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.LocationOn, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(LocalStrings.current.mkCollectorAddress, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(it, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountsCard(order: MarketOrder, asSeller: Boolean) {
    val strings = LocalStrings.current
    PharmaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AmountLine(strings.mkTotal, strings.mkPrice(money(order.totalAmount)), emphasized = !asSeller)
            // Commission comes out of the seller's side, so only the seller sees the breakdown.
            if (asSeller) {
                AmountLine(
                    strings.mkCommission(formatDecimal(order.commissionPct, if (order.commissionPct % 1.0 == 0.0) 0 else 1)),
                    "- ${strings.mkPrice(money(order.commissionAmount))}"
                )
                HorizontalDivider()
                AmountLine(strings.mkYouReceive, strings.mkPrice(money(order.netAmount)), emphasized = true)
            }
        }
    }
}

@Composable
private fun AmountLine(label: String, value: String, emphasized: Boolean = false) {
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
private fun OrderActions(
    order: MarketOrder,
    asSeller: Boolean,
    isActing: Boolean,
    onAccept: () -> Unit,
    onReady: () -> Unit,
    onConfirm: (PendingConfirm) -> Unit
) {
    val strings = LocalStrings.current
    val status = order.status
    // Nothing left to do on a finished order, and the buyer's collector form lives in the body.
    val info: String? = when {
        status == MarketOrderStatus.COMPLETED -> strings.mkOrderCompletedMsg
        status.isTerminal || status == MarketOrderStatus.UNKNOWN -> return
        asSeller -> when (status) {
            MarketOrderStatus.PENDING_COLLECTOR -> strings.mkWaitingCollector
            MarketOrderStatus.ACCEPTED -> strings.mkSellerPrepare
            MarketOrderStatus.READY_FOR_PICKUP -> strings.mkReadyForPickupSeller
            else -> null
        }
        else -> when (status) {
            MarketOrderStatus.PENDING_SELLER -> strings.mkWaitingSeller
            MarketOrderStatus.ACCEPTED -> strings.mkWaitingReady
            MarketOrderStatus.READY_FOR_PICKUP -> strings.mkReadyForPickupBuyer
            else -> null
        }
    }
    Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            info?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.fillMaxWidth())
            }
            if (asSeller) {
                when (status) {
                    MarketOrderStatus.PENDING_SELLER -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PharmaOutlinedButton(
                            text = strings.mkReject,
                            onClick = { onConfirm(PendingConfirm.REJECT) },
                            enabled = !isActing,
                            modifier = Modifier.weight(1f)
                        )
                        PharmaButton(text = strings.mkAccept, onClick = onAccept, isLoading = isActing, modifier = Modifier.weight(1f))
                    }
                    MarketOrderStatus.ACCEPTED -> {
                        PharmaButton(text = strings.mkMarkReady, onClick = onReady, isLoading = isActing, modifier = Modifier.fillMaxWidth())
                        PharmaOutlinedButton(
                            text = strings.mkCancelOrder,
                            onClick = { onConfirm(PendingConfirm.SELLER_CANCEL) },
                            enabled = !isActing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    MarketOrderStatus.READY_FOR_PICKUP -> PharmaButton(
                        text = strings.mkComplete,
                        onClick = { onConfirm(PendingConfirm.COMPLETE) },
                        isLoading = isActing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    else -> Unit
                }
            } else if (status.buyerCanCancel) {
                PharmaOutlinedButton(
                    text = strings.mkCancelOrder,
                    onClick = { onConfirm(PendingConfirm.BUYER_CANCEL) },
                    enabled = !isActing,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
