package com.pharmatrade.feature.catalog.presentation.sellers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.*
import com.pharmatrade.core.ui.theme.*

@Composable
fun SellerBrowseScreen(
    viewModel: SellerBrowseViewModel,
    onNavigateToSellerDrugs: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    onLogout: () -> Unit,
    cartItemCount: Int = 0,
    showTopBar: Boolean = true,
    onSearchTap: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSearchMode = uiState.searchQuery.isNotBlank()
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        if (showTopBar) {
        // Header
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
                    Column {
                        Text(strings.appTitle, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(strings.catalogBrowseDrugsSellers, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                    Row {
                        BadgedBox(
                            badge = {
                                if (cartItemCount > 0) Badge { Text("$cartItemCount") }
                            }
                        ) {
                            IconButton(onClick = onNavigateToCart) {
                                Icon(Icons.Filled.ShoppingCart, strings.navCart, tint = Color.White)
                            }
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, strings.catalogLogoutContentDescription, tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text(strings.catalogSearchPlaceholder, color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.White) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Clear, null, tint = Color.White)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    )
                )
            }
        }
        } // end if (showTopBar)

        // Tappable search bar when embedded — navigates to dedicated search screen
        if (!showTopBar) {
            TappableSearchBar(
                hint = strings.catalogSearchSellersPlaceholder,
                onClick = onSearchTap,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        when {
            uiState.isLoading -> LoadingScreen()
            uiState.error != null -> ErrorScreen(uiState.error!!, onRetry = viewModel::loadSellers)
            isSearchMode -> SearchResultsList(
                results = uiState.searchResults,
                isSearching = uiState.isSearching,
                onSellerClick = onNavigateToSellerDrugs
            )
            else -> SellerList(
                sellers = uiState.sellers,
                onSellerClick = onNavigateToSellerDrugs
            )
        }
    }
}

@Composable
private fun SellerList(sellers: List<Seller>, onSellerClick: (String) -> Unit) {
    val strings = LocalStrings.current
    if (sellers.isEmpty()) {
        EmptyState(
            title = strings.catalogNoSellersFound,
            message = strings.catalogNoSellersMessage,
            icon = Icons.Filled.Store
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(title = strings.catalogAvailableSellers(sellers.size), modifier = Modifier.padding(bottom = 4.dp))
        }
        items(sellers, key = { it.id }) { seller ->
            SellerCard(seller = seller, onClick = { onSellerClick(seller.id) })
        }
    }
}

@Composable
private fun SellerCard(seller: Seller, onClick: () -> Unit) {
    val strings = LocalStrings.current
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocalPharmacy, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(seller.businessName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (seller.isVerified) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Filled.Verified, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(seller.ownerName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                RatingRow(seller.rating)
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = DividerGray)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(icon = Icons.Filled.LocationOn, text = seller.location, modifier = Modifier.weight(1f))
                InfoChip(icon = Icons.Filled.Phone, text = seller.phone, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ShoppingCart, null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = strings.cartMinOrder(formatDecimal(seller.minimumOrderAmount, 0)),
                            style = MaterialTheme.typography.labelMedium,
                            color = WarningAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = strings.catalogCompletedOrders(seller.totalSales),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(strings.catalogBrowse)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardGray)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
    }
}

@Composable
private fun SearchResultsList(
    results: List<SellerListing>,
    isSearching: Boolean,
    onSellerClick: (String) -> Unit
) {
    val strings = LocalStrings.current
    if (isSearching) {
        LoadingScreen()
        return
    }
    if (results.isEmpty()) {
        EmptyState(
            title = strings.catalogNoResults,
            message = strings.catalogNoResultsMessage,
            icon = Icons.Filled.SearchOff
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(strings.catalogSearchResults(results.size), modifier = Modifier.padding(bottom = 4.dp))
        }
        items(results, key = { it.id }) { listing ->
            SearchResultCard(listing = listing, onClick = { onSellerClick(listing.seller.id) })
        }
    }
}

@Composable
private fun SearchResultCard(listing: SellerListing, onClick: () -> Unit) {
    val strings = LocalStrings.current
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(listing.drug.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(listing.drug.genericName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Store, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(listing.seller.businessName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (listing.hasDiscount) DiscountBadge(listing.discountPercentage)
                Text("EGP ${formatDecimal(listing.finalPrice, 2)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
                    Text(strings.catalogViewSeller, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
