package com.pharmatrade.feature.pharmacyorder.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.DiscountBadge
import com.pharmatrade.core.ui.components.PharmaCard
import com.pharmatrade.core.ui.components.SupplierAvatar
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem

// ── Grouping ─────────────────────────────────────────────────────────────────

// One Home row: every supplier offering the same drug, cheapest first.
data class CatalogDrugGroup(val key: String, val offers: List<SupplierCatalogItem>) {
    val first: SupplierCatalogItem get() = offers.first()
    val minPrice: Double get() = offers.minOf { it.pharmacistPrice }
    val maxPrice: Double get() = offers.maxOf { it.pharmacistPrice }
}

// Keeps the list's order (a drug sits where its first offer appeared). Items without a real drug
// id stay on their own rather than all collapsing into one "0" group.
internal fun List<SupplierCatalogItem>.groupByDrug(): List<CatalogDrugGroup> =
    groupBy { if (it.drugId.isBlank() || it.drugId == "0") "item_${it.id}" else "drug_${it.drugId}" }
        .map { (key, offers) -> CatalogDrugGroup(key, offers.sortedBy { it.pharmacistPrice }) }

// ── Home card for a drug with several suppliers ──────────────────────────────

@Composable
internal fun DrugGroupCard(
    group: CatalogDrugGroup,
    addedQuantity: Int,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val item = group.first
    val totalStock = group.offers.sumOf { it.quantityAvailable }

    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StackedSupplierAvatars(group.offers)

            Column(modifier = Modifier.weight(1f)) {
                Text(item.drugName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(11.dp))
                    Text(strings.pharmacySuppliersCount(group.offers.size), style = MaterialTheme.typography.labelSmall, color = PrimaryBlue, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                Spacer(Modifier.height(3.dp))
                StockChip(outOfStock = totalStock <= 0, lowStock = totalStock in 1..9, quantity = totalStock)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "EGP ${formatDecimal(group.minPrice, 2)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (group.maxPrice > group.minPrice) {
                    Text(
                        "– ${formatDecimal(group.maxPrice, 2)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            if (addedQuantity > 0) {
                // How many of this drug are in the cart, across all its suppliers.
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(SecondaryGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$addedQuantity", style = MaterialTheme.typography.labelSmall, color = SecondaryGreenDark, fontWeight = FontWeight.Bold)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

// The first two suppliers' avatars overlapping, in the same 42dp slot a single avatar takes.
@Composable
private fun StackedSupplierAvatars(offers: List<SupplierCatalogItem>) {
    Box(modifier = Modifier.size(42.dp)) {
        offers.take(2).forEachIndexed { index, offer ->
            SupplierAvatar(
                name = offer.supplierName,
                logoUrl = offer.supplierLogoUrl,
                size = 30.dp,
                modifier = Modifier
                    .align(if (index == 0) Alignment.TopStart else Alignment.BottomEnd)
                    .border(2.dp, SurfaceWhite, CircleShape)
            )
        }
    }
}

// ── Suppliers screen ─────────────────────────────────────────────────────────

// Every supplier offering one drug, cheapest first, each with its own add-to-cart. Drawn on top of
// Home (not a separate nav destination) so it shares Home's ViewModel and cart — the same reason
// as PharmacySearchScreen — and Home keeps its scroll position underneath.
@Composable
fun DrugOffersScreen(viewModel: PharmacyHomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.openedDrugId == null) return
    val strings = LocalStrings.current
    val offers = uiState.drugOffers
    val drugName = offers.firstOrNull()?.drugName.orEmpty()

    uiState.pendingCatalogItem?.let { pending ->
        CatalogQuantityDialog(
            item = pending,
            initialQuantity = uiState.cartQuantities[pending.id] ?: 1,
            onDismiss = viewModel::dismissCatalogDialog,
            onConfirm = viewModel::confirmAddCatalogItem
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            // Home stays composed underneath — swallow taps so none reach it.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Surface(shadowElevation = 2.dp, color = TopBarContainer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::closeDrugOffers) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.commonBack, tint = TopBarContent)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(drugName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TopBarContent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(strings.pharmacySuppliersCount(offers.size), style = MaterialTheme.typography.labelSmall, color = TopBarContent.copy(alpha = 0.8f))
                }
                if (uiState.isLoadingDrugOffers) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = TopBarContent)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.catalogActionError?.let { error ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ErrorRedContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorRed, modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::dismissCatalogActionError, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = strings.commonDismiss, tint = ErrorRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            items(offers, key = { it.id }) { offer ->
                SupplierOfferCard(
                    offer = offer,
                    addedQuantity = uiState.cartQuantities[offer.id] ?: 0,
                    onQuickAdd = { viewModel.quickAddCatalogItem(offer) },
                    onIncrease = { viewModel.incrementCartItem(offer) },
                    onDecrease = { viewModel.decrementCartItem(offer) },
                    onOpenDialog = { viewModel.onCatalogItemTapped(offer) }
                )
            }
        }
    }
}

// One supplier's offer for the open drug: the supplier leads (the drug is in the header).
@Composable
private fun SupplierOfferCard(
    offer: SupplierCatalogItem,
    addedQuantity: Int,
    onQuickAdd: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onOpenDialog: () -> Unit
) {
    val strings = LocalStrings.current
    val outOfStock = offer.quantityAvailable <= 0
    val lowStock = !outOfStock && offer.quantityAvailable < 10

    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !outOfStock, onClick = onOpenDialog)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SupplierAvatar(name = offer.supplierName, logoUrl = offer.supplierLogoUrl)

            Column(modifier = Modifier.weight(1f)) {
                Text(offer.supplierName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                StockChip(outOfStock = outOfStock, lowStock = lowStock, quantity = offer.quantityAvailable)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "EGP ${formatDecimal(offer.publicPrice, 2)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint,
                    textDecoration = TextDecoration.LineThrough
                )
                Text(
                    "EGP ${formatDecimal(offer.pharmacistPrice, 2)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (offer.discountPct > 0) {
                    Spacer(Modifier.height(2.dp))
                    DiscountBadge(discountPercentage = offer.discountPct)
                }
            }

            if (addedQuantity > 0) {
                CartQuantityStepper(
                    quantity = addedQuantity,
                    canIncrease = addedQuantity < offer.quantityAvailable,
                    onIncrease = onIncrease,
                    onDecrease = onDecrease
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (!outOfStock) PrimaryBlue else DividerGray)
                        .clickable(enabled = !outOfStock, onClick = onQuickAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = strings.commonAddToCart, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
