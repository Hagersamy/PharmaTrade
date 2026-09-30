package com.pharmatrade.feature.cart.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.feature.cart.domain.model.CartItem
import com.pharmatrade.feature.cart.domain.model.OrderValidation
import com.pharmatrade.core.ui.components.*
import com.pharmatrade.core.ui.theme.*

@Composable
fun CartScreen(
    viewModel: CartViewModel,
    onNavigateBack: () -> Unit,
    onOrderSuccess: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    LaunchedEffect(uiState.orderPlacedId) {
        uiState.orderPlacedId?.let {
            viewModel.onOrderConfirmed()
            onOrderSuccess(it)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(
            title = strings.cartTitle(uiState.cart.getTotalItems()),
            onNavigateBack = onNavigateBack,
            actions = {
                if (!uiState.cart.isEmpty()) {
                    TextButton(onClick = viewModel::clearCart) {
                        Text(strings.commonClear, color = Color.White)
                    }
                }
            }
        )

        if (uiState.cart.isEmpty()) {
            EmptyState(
                title = strings.cartEmptyTitle,
                message = strings.cartEmptyMessage,
                icon = Icons.Filled.ShoppingCart,
                action = {
                    PharmaButton(text = strings.cartBrowseSellers, onClick = onNavigateBack)
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Minimum order warnings at the top
                if (uiState.validationErrors.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.validationErrors.forEach { validation ->
                                if (validation is OrderValidation.BelowMinimum) {
                                    MinimumOrderWarning(
                                        sellerName = validation.sellerName,
                                        currentAmount = validation.currentAmount,
                                        minimumAmount = validation.minimumAmount,
                                        shortfall = validation.shortfall
                                    )
                                }
                            }
                        }
                    }
                }

                // Group by seller
                uiState.cart.itemsBySeller.forEach { (sellerId, sellerItems) ->
                    if (sellerItems.isEmpty()) return@forEach
                    val seller = sellerItems.first().listing.seller
                    val sellerSubtotal = uiState.cart.getSellerSubtotal(sellerId)
                    val isBelowMinimum = sellerSubtotal < seller.minimumOrderAmount

                    item(key = "header_$sellerId") {
                        SellerGroupHeader(
                            businessName = seller.businessName,
                            subtotal = sellerSubtotal,
                            minimumOrder = seller.minimumOrderAmount,
                            isBelowMinimum = isBelowMinimum
                        )
                    }

                    items(sellerItems, key = { "item_${it.listing.id}" }) { cartItem ->
                        CartItemCard(
                            item = cartItem,
                            onIncrease = { viewModel.updateQuantity(cartItem.listing.id, cartItem.quantity + 1) },
                            onDecrease = {
                                if (cartItem.quantity > 1)
                                    viewModel.updateQuantity(cartItem.listing.id, cartItem.quantity - 1)
                                else
                                    viewModel.removeItem(cartItem.listing.id)
                            },
                            onRemove = { viewModel.removeItem(cartItem.listing.id) }
                        )
                    }

                    item(key = "divider_$sellerId") {
                        Divider(color = DividerGray, thickness = 2.dp)
                    }
                }
            }

            // Order summary footer
            Surface(shadowElevation = 8.dp, color = SurfaceWhite) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (uiState.error != null) {
                        Text(
                            uiState.error!!,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(strings.cartOrderTotal, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(
                                "EGP ${formatDecimal(uiState.cart.getTotalAmount(), 2)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        PharmaButton(
                            text = strings.cartPlaceOrder,
                            onClick = viewModel::placeOrder,
                            isLoading = uiState.isPlacingOrder,
                            enabled = uiState.validationErrors.isEmpty() && !uiState.cart.isEmpty()
                        )
                    }
                    if (uiState.validationErrors.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            strings.cartResolveMinimumWarning,
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningAmber,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerGroupHeader(
    businessName: String,
    subtotal: Double,
    minimumOrder: Double,
    isBelowMinimum: Boolean
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isBelowMinimum) WarningAmberContainer else SecondaryGreenContainer)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isBelowMinimum) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isBelowMinimum) WarningAmber else SecondaryGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    businessName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isBelowMinimum) WarningAmberOnContainer else SecondaryGreenDark
                )
                Text(
                    strings.cartMinOrder(formatDecimal(minimumOrder, 0)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isBelowMinimum) WarningAmberOnContainer else SecondaryGreenDark
                )
            }
        }
        Text(
            "EGP ${formatDecimal(subtotal, 2)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isBelowMinimum) WarningAmber else SecondaryGreenDark
        )
    }
}

@Composable
private fun CartItemCard(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val strings = LocalStrings.current
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Medication, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(item.listing.drug.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(item.listing.drug.genericName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "EGP ${formatDecimal(item.listing.finalPrice, 2)} × ${item.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        " = EGP ${formatDecimal(item.subtotal, 2)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                if (item.listing.hasDiscount) {
                    DiscountBadge(item.listing.discountPercentage)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CardGray)
                    ) {
                        Icon(
                            Icons.Filled.Remove, null,
                            modifier = Modifier.size(14.dp),
                            tint = TextPrimary
                        )
                    }
                    Text(
                        "${item.quantity}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = onIncrease,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp), tint = Color.White)
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onRemove,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(strings.commonRemove, style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                }
            }
        }
    }
}
