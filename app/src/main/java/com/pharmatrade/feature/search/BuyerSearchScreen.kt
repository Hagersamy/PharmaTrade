package com.pharmatrade.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.model.Seller
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.ui.components.*
import com.pharmatrade.core.ui.theme.*

@Composable
fun BuyerSearchScreen(
    viewModel: BuyerSearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSellerDrugs: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {

        // Top bar with search field
        Surface(color = PrimaryBlue, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Search sellers, drugs...", color = Color.White.copy(alpha = 0.6f)) },
                    trailingIcon = {
                        if (uiState.query.isNotBlank()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Filled.Clear, null, tint = Color.White)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    )
                )
                Spacer(Modifier.width(8.dp))
            }
        }

        // Filter chips + sort icon
        var showSortMenu by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                modifier = Modifier.weight(1f),
                selected = uiState.filterSellers,
                onClick = viewModel::toggleFilterSellers,
                label = { Text("Sellers") },
                leadingIcon = {
                    Icon(
                        if (uiState.filterSellers) Icons.Filled.CheckCircle else Icons.Filled.Store,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlueContainer,
                    selectedLabelColor = PrimaryBlue,
                    selectedLeadingIconColor = PrimaryBlue
                )
            )
            FilterChip(
                modifier = Modifier.weight(1f),
                selected = uiState.filterDrugs,
                onClick = viewModel::toggleFilterDrugs,
                label = { Text("Drugs") },
                leadingIcon = {
                    Icon(
                        if (uiState.filterDrugs) Icons.Filled.CheckCircle else Icons.Filled.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlueContainer,
                    selectedLabelColor = PrimaryBlue,
                    selectedLeadingIconColor = PrimaryBlue
                )
            )
            Box {
                IconButton(
                    onClick = { if (uiState.filterDrugs) showSortMenu = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sort,
                        contentDescription = "Sort drugs",
                        tint = if (uiState.filterDrugs) PrimaryBlue else DividerGray
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Best Price") },
                        leadingIcon = { Icon(Icons.Filled.TrendingDown, contentDescription = null) },
                        trailingIcon = if (uiState.drugSortOrder == DrugSortOrder.BEST_PRICE) {
                            { Icon(Icons.Filled.Check, contentDescription = null, tint = PrimaryBlue) }
                        } else null,
                        onClick = {
                            viewModel.onDrugSortChange(DrugSortOrder.BEST_PRICE)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("A–Z") },
                        leadingIcon = { Icon(Icons.Filled.SortByAlpha, contentDescription = null) },
                        trailingIcon = if (uiState.drugSortOrder == DrugSortOrder.A_Z) {
                            { Icon(Icons.Filled.Check, contentDescription = null, tint = PrimaryBlue) }
                        } else null,
                        onClick = {
                            viewModel.onDrugSortChange(DrugSortOrder.A_Z)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = DividerGray)

        // Results
        when {
            uiState.query.isBlank() -> SearchHint()
            uiState.isLoadingSellers && uiState.isSearchingDrugs -> LoadingScreen()
            else -> SearchResults(
                uiState = uiState,
                onSellerClick = onNavigateToSellerDrugs
            )
        }
    }
}

@Composable
private fun SearchHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = DividerGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Type to search sellers or drugs",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SearchResults(
    uiState: BuyerSearchUiState,
    onSellerClick: (String) -> Unit
) {
    val hasSellers = uiState.filterSellers && uiState.filteredSellers.isNotEmpty()
    val hasDrugs = uiState.filterDrugs && uiState.filteredListings.isNotEmpty()
    val stillSearching = uiState.isSearchingDrugs

    if (!hasSellers && !hasDrugs && !stillSearching) {
        EmptyState(
            title = "No Results",
            message = "Nothing matched \"${uiState.query}\". Try different keywords.",
            icon = Icons.Filled.SearchOff
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sellers section
        if (uiState.filterSellers) {
            if (hasSellers) {
                item {
                    SectionLabel(
                        icon = Icons.Filled.Store,
                        text = "Sellers (${uiState.filteredSellers.size})"
                    )
                }
                items(uiState.filteredSellers, key = { "s_${it.id}" }) { seller ->
                    SellerResultCard(seller = seller, onClick = { onSellerClick(seller.id) })
                }
            } else if (!stillSearching) {
                item {
                    SectionLabel(icon = Icons.Filled.Store, text = "Sellers")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "No sellers matched",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        // Drugs section
        if (uiState.filterDrugs) {
            item {
                SectionLabel(
                    icon = Icons.Filled.Medication,
                    text = if (stillSearching) "Drugs (searching...)"
                    else "Drugs (${uiState.filteredListings.size})"
                )
            }
            if (stillSearching) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            } else if (hasDrugs) {
                items(uiState.filteredListings, key = { "d_${it.id}" }) { listing ->
                    DrugResultCard(listing = listing, onSellerClick = { onSellerClick(listing.seller.id) })
                }
            } else {
                item {
                    Text(
                        "No drugs matched",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun SellerResultCard(seller: Seller, onClick: () -> Unit) {
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocalPharmacy, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(seller.businessName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (seller.isVerified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.Verified, null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(seller.location, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            TextButton(onClick = onClick) { Text("Browse") }
        }
    }
}

@Composable
private fun DrugResultCard(listing: SellerListing, onSellerClick: () -> Unit) {
    PharmaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(listing.drug.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(listing.drug.genericName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Store, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(listing.seller.businessName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (listing.hasDiscount) DiscountBadge(listing.discountPercentage)
                Text(
                    "EGP ${String.format("%.2f", listing.finalPrice)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                TextButton(onClick = onSellerClick, contentPadding = PaddingValues(0.dp)) {
                    Text("View Seller", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
