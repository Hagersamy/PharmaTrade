package com.pharmatrade.feature.catalog.presentation.drugs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.model.DrugCategory
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.*
import com.pharmatrade.core.ui.theme.*

@Composable
fun SellerDrugsScreen(
    viewModel: SellerDrugsViewModel,
    sellerId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    onAddToCart: (SellerListing) -> Unit,
    cartItemCount: Int = 0
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedCategory by remember { mutableStateOf<DrugCategory?>(null) }

    LaunchedEffect(sellerId) { viewModel.loadSeller(sellerId) }

    LaunchedEffect(uiState.addedToCartListing) {
        uiState.addedToCartListing?.let {
            snackbarHostState.showSnackbar("${it.drug.name} added to cart")
            viewModel.onAddedToCartHandled()
        }
    }

    val filteredListings = if (selectedCategory != null) {
        uiState.listings.filter { it.drug.category == selectedCategory }
    } else {
        uiState.listings
    }

    val categories = uiState.listings.map { it.drug.category }.distinct()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(BackgroundGray)) {
            // Seller header
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(
                                    uiState.seller?.businessName ?: "Loading...",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.seller != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, null, tint = WarningAmber, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "${uiState.seller!!.rating} · ${uiState.seller!!.location}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                        BadgedBox(badge = { if (cartItemCount > 0) Badge { Text("$cartItemCount") } }) {
                            IconButton(onClick = onNavigateToCart) {
                                Icon(Icons.Filled.ShoppingCart, "Cart", tint = Color.White)
                            }
                        }
                    }

                    if (uiState.seller != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(WarningAmberContainer)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ShoppingCart, null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Minimum Order: EGP ${formatDecimal(uiState.seller!!.minimumOrderAmount, 0)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> LoadingScreen()
                uiState.error != null -> ErrorScreen(uiState.error!!, onRetry = { viewModel.loadSeller(sellerId) })
                uiState.listings.isEmpty() -> EmptyState(
                    title = "No Listings",
                    message = "This seller has no active drug listings",
                    icon = Icons.Filled.Inventory
                )
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (categories.size > 1) {
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        FilterChip(
                                            selected = selectedCategory == null,
                                            onClick = { selectedCategory = null },
                                            label = { Text("All") }
                                        )
                                    }
                                    items(categories) { cat ->
                                        FilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = {
                                                selectedCategory = if (selectedCategory == cat) null else cat
                                            },
                                            label = { Text(cat.displayName) }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            SectionHeader(
                                title = "${filteredListings.size} Drugs Available",
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(filteredListings, key = { it.id }) { listing ->
                            DrugListingCard(
                                listing = listing,
                                onAddToCart = {
                                    onAddToCart(listing)
                                    viewModel.onAddedToCart(listing)
                                }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrugListingCard(listing: SellerListing, onAddToCart: () -> Unit) {
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(when (listing.drug.category) {
                                DrugCategory.ANTIBIOTIC -> Color(0xFFFFE4E1)
                                DrugCategory.CARDIOVASCULAR -> Color(0xFFFFEBEB)
                                DrugCategory.DIABETES -> Color(0xFFE0F2FE)
                                DrugCategory.ANALGESIC -> Color(0xFFFEF9C3)
                                else -> PrimaryBlueContainer
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Medication, null,
                            tint = when (listing.drug.category) {
                                DrugCategory.ANTIBIOTIC -> Color(0xFFDC2626)
                                DrugCategory.CARDIOVASCULAR -> Color(0xFFE11D48)
                                DrugCategory.DIABETES -> Color(0xFF0284C7)
                                DrugCategory.ANALGESIC -> Color(0xFFCA8A04)
                                else -> PrimaryBlue
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(listing.drug.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(listing.drug.genericName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(listing.drug.category.displayName, style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                    }
                }
                if (listing.hasDiscount) DiscountBadge(listing.discountPercentage)
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = DividerGray)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "EGP ${formatDecimal(listing.finalPrice, 2)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "/ ${listing.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (listing.hasDiscount) {
                        Text(
                            text = "Was EGP ${formatDecimal(listing.pricePerUnit, 2)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Inventory2, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${listing.quantityAvailable} units available · Exp: ${listing.expiryDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Button(
                    onClick = onAddToCart,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.AddShoppingCart, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
    }
}
