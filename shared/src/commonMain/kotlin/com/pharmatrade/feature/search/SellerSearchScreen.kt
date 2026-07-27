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
import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.model.SellerListing
import com.pharmatrade.core.common.util.formatDecimal
import com.pharmatrade.core.ui.components.*
import com.pharmatrade.core.ui.theme.*

@Composable
fun SellerSearchScreen(
    viewModel: SellerSearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onAddListing: () -> Unit
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
                    placeholder = { Text("Search the drug catalog...", color = Color.White.copy(alpha = 0.6f)) },
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

        // Results: browse own listings when the query is blank, otherwise a live GET
        // /drugs?search= against the full catalog (not scoped to what's already listed).
        when {
            uiState.query.isBlank() -> if (uiState.isLoading) LoadingScreen() else AllListingsContent(
                listings = uiState.allListings,
                onEdit = onNavigateToEdit
            )
            uiState.isSearchingCatalog && uiState.catalogResults.isEmpty() -> LoadingScreen()
            uiState.catalogSearchError != null -> ErrorScreen(message = uiState.catalogSearchError!!, onRetry = { viewModel.onQueryChange(uiState.query) })
            uiState.catalogResults.isEmpty() -> EmptyState(
                title = "No Results",
                message = "No catalog drugs matched \"${uiState.query}\"",
                icon = Icons.Filled.SearchOff
            )
            else -> DrugCatalogResults(
                drugs = uiState.catalogResults,
                ownListings = uiState.allListings,
                onEdit = onNavigateToEdit,
                onAddListing = onAddListing
            )
        }
    }
}

@Composable
private fun DrugCatalogResults(
    drugs: List<Drug>,
    ownListings: List<SellerListing>,
    onEdit: (String) -> Unit,
    onAddListing: () -> Unit
) {
    // Matched by drug name against the seller's own listings already loaded on this screen —
    // there's no backend call that maps a catalog id straight to "do I already list this".
    val listingByDrugName = remember(ownListings) {
        ownListings.associateBy { it.drug.name.trim().lowercase() }
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader("Catalog results (${drugs.size})", modifier = Modifier.padding(bottom = 4.dp))
        }
        items(drugs, key = { it.id }) { drug ->
            val existingListing = listingByDrugName[drug.name.trim().lowercase()]
            DrugCatalogSearchCard(
                drug = drug,
                existingListing = existingListing,
                onEdit = { onEdit(existingListing!!.id) },
                onAddListing = onAddListing
            )
        }
    }
}

@Composable
private fun DrugCatalogSearchCard(
    drug: Drug,
    existingListing: SellerListing?,
    onEdit: () -> Unit,
    onAddListing: () -> Unit
) {
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
                    Icon(Icons.Filled.Medication, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(drug.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    val subtitle = listOfNotNull(
                        drug.genericName.takeIf { it.isNotBlank() },
                        drug.manufacturer.takeIf { it.isNotBlank() },
                        listOf(drug.dosageForm, drug.strength).filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    if (existingListing != null) {
                        Text(
                            "Already listed · EGP ${formatDecimal(existingListing.finalPrice, 2)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryGreenDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (existingListing != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit your listing", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                }
            } else {
                IconButton(onClick = onAddListing) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = "Add as listing", tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun AllListingsContent(
    listings: List<SellerListing>,
    onEdit: (String) -> Unit,
    headerText: String = "All Listings (${listings.size})"
) {
    if (listings.isEmpty()) {
        EmptyState(
            title = "No Listings",
            message = "You have no drug listings yet",
            icon = Icons.Filled.Inventory
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(headerText, modifier = Modifier.padding(bottom = 4.dp))
        }
        items(listings, key = { it.id }) { listing ->
            SellerListingSearchCard(listing = listing, onEdit = { onEdit(listing.id) })
        }
    }
}

@Composable
private fun SellerListingSearchCard(listing: SellerListing, onEdit: () -> Unit) {
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
                    Icon(Icons.Filled.Medication, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        listing.drug.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        listing.drug.genericName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "EGP ${formatDecimal(listing.finalPrice, 2)}  ·  ${listing.quantityAvailable} ${listing.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            }
        }
    }
}
