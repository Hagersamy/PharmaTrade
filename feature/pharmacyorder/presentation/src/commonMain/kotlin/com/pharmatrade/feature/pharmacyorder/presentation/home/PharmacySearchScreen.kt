package com.pharmatrade.feature.pharmacyorder.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.theme.*

// Full-screen drug search, opened from the Home top bar's search icon. Shares Home's ViewModel
// (and so its cart) — it's shown in place of Home rather than as a separate nav destination,
// which would get its own ViewModel and a second, diverging copy of the cart.
//
// Empty box → the user's recent searches (or an empty state if there are none); typing → the
// live ?search= results, with the same add-to-cart actions as Home.
@Composable
fun PharmacySearchScreen(
    viewModel: PharmacyHomeViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Open with the keyboard up, ready to type.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // Leaving search puts Home back on the full catalog.
    DisposableEffect(Unit) {
        onDispose { viewModel.onCatalogFilterChange("") }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        Surface(shadowElevation = 2.dp, color = TopBarContainer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.commonBack, tint = TopBarContent)
                }
                TextField(
                    value = uiState.catalogFilter,
                    onValueChange = viewModel::onCatalogFilterChange,
                    placeholder = { Text(strings.pharmacySearchDrugName) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextSecondary) },
                    trailingIcon = {
                        if (uiState.catalogFilter.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onCatalogFilterChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = strings.commonClear, tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.onSearchSubmitted()
                        focusManager.clearFocus()
                    }),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = PrimaryBlue
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).focusRequester(focusRequester)
                )
            }
        }

        val bodyModifier = Modifier.weight(1f).fillMaxWidth()
        when {
            uiState.catalogFilter.isNotBlank() -> CatalogList(
                viewModel = viewModel,
                bottomPadding = 0.dp,
                modifier = bodyModifier.navigationBarsPadding()
            )
            uiState.recentSearches.isEmpty() -> EmptyState(
                title = strings.pharmacyNoRecentSearches,
                message = strings.pharmacyNoRecentSearchesMessage,
                icon = Icons.Filled.History,
                modifier = bodyModifier
            )
            else -> RecentSearches(
                searches = uiState.recentSearches,
                onSelect = { query ->
                    viewModel.onRecentSearchSelected(query)
                    focusManager.clearFocus()
                },
                onRemove = viewModel::removeRecentSearch,
                onClearAll = viewModel::clearRecentSearches,
                modifier = bodyModifier
            )
        }
    }
}

@Composable
private fun RecentSearches(
    searches: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.pharmacyRecentSearches,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClearAll) {
                    Text(strings.commonClearAll, color = PrimaryBlue)
                }
            }
        }
        items(searches, key = { it }) { query ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(query) }
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(
                    query,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(query) }) {
                    Icon(Icons.Filled.Close, contentDescription = strings.commonClear, tint = TextHint, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
