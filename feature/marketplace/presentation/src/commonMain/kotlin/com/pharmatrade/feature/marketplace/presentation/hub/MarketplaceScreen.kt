package com.pharmatrade.feature.marketplace.presentation.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import com.pharmatrade.core.common.hints.DismissedHintsStore
import com.pharmatrade.core.ui.theme.PrimaryBlueContainer
import com.pharmatrade.core.ui.theme.TextPrimary
import com.pharmatrade.core.ui.theme.TextSecondary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.io.rememberFilePickerLauncher
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.components.LocalBottomBarInset
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.LoadingScreen
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.rememberToast
import com.pharmatrade.core.ui.theme.BackgroundGray
import com.pharmatrade.core.ui.theme.PrimaryBlue
import com.pharmatrade.feature.marketplace.presentation.components.CollectionCard
import com.pharmatrade.feature.marketplace.presentation.components.InventoryRow
import com.pharmatrade.feature.marketplace.presentation.components.MarketOrderCard

// Buying tabs first (what most visits are for), then the selling side.
enum class MarketTab { BROWSE, PURCHASES, COLLECTIONS, INVENTORY, SALES }

// Shown as the pharmacy Home shell's Marketplace tab, under Home's own top bar and above its
// floating bottom bar.
@Composable
fun MarketplaceScreen(
    viewModels: MarketplaceTabViewModels,
    selectedTab: MarketTab,
    onTabSelected: (MarketTab) -> Unit,
    onOpenCollection: (collectionId: String, mine: Boolean) -> Unit,
    onOpenOrder: (orderId: String, asSeller: Boolean) -> Unit
) {
    val strings = LocalStrings.current
    val tabs = listOf(
        MarketTab.BROWSE to strings.mkTabBrowse,
        MarketTab.PURCHASES to strings.mkTabPurchases,
        MarketTab.COLLECTIONS to strings.mkTabCollections,
        MarketTab.INVENTORY to strings.mkTabInventory,
        MarketTab.SALES to strings.mkTabSales
    )
    Column(Modifier.fillMaxSize().background(BackgroundGray)) {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab },
            containerColor = BackgroundGray,
            edgePadding = 8.dp,
            divider = {}
        ) {
            tabs.forEach { (tab, label) ->
                Tab(
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(label) }
                )
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (selectedTab) {
                MarketTab.BROWSE -> BrowseTab(viewModels.browse, onOpenCollection = { onOpenCollection(it, false) })
                MarketTab.PURCHASES -> OrdersTab(
                    viewModel = viewModels.purchases,
                    asSeller = false,
                    emptyTitle = strings.mkPurchasesEmptyTitle,
                    emptyMessage = strings.mkPurchasesEmptyMessage,
                    onOpenOrder = { onOpenOrder(it, false) }
                )
                MarketTab.COLLECTIONS -> CollectionsTab(viewModels.collections, onOpenCollection = { onOpenCollection(it, true) })
                MarketTab.INVENTORY -> InventoryTab(viewModels.inventory)
                MarketTab.SALES -> OrdersTab(
                    viewModel = viewModels.sales,
                    asSeller = true,
                    emptyTitle = strings.mkSalesEmptyTitle,
                    emptyMessage = strings.mkSalesEmptyMessage,
                    onOpenOrder = { onOpenOrder(it, true) }
                )
            }
        }
    }
}

// Shared loading / error / empty handling for every tab's list.
@Composable
private fun TabList(
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    onRetry: () -> Unit,
    emptyTitle: String,
    emptyMessage: String,
    emptyIcon: ImageVector,
    header: (LazyListScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
    onReachedEnd: (() -> Unit)? = null
) {
    when {
        isLoading -> LoadingScreen()
        error != null && isEmpty -> ErrorScreen(message = error, onRetry = onRetry)
        else -> {
            val listState = rememberLazyListState()
            if (onReachedEnd != null) {
                val reachedEnd by remember {
                    derivedStateOf {
                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        last >= listState.layoutInfo.totalItemsCount - 3
                    }
                }
                LaunchedEffect(reachedEnd) { if (reachedEnd) onReachedEnd() }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // Last item clears the floating glass bottom bar.
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + LocalBottomBarInset.current),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                header?.invoke(this)
                if (isEmpty) {
                    item { EmptyState(title = emptyTitle, message = emptyMessage, icon = emptyIcon, modifier = Modifier.padding(top = 32.dp)) }
                } else {
                    content()
                }
            }
        }
    }
}

@Composable
private fun BrowseTab(viewModel: MarketBrowseViewModel, onOpenCollection: (String) -> Unit) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tipDismissed by remember { DismissedHintsStore.observe(DismissedHintsStore.MARKETPLACE_BROWSE) }
        .collectAsStateWithLifecycle(initialValue = true)
    TabList(
        isLoading = state.isLoading || !state.loaded,
        error = state.error,
        isEmpty = state.collections.isEmpty(),
        onRetry = viewModel::refresh,
        emptyTitle = strings.mkBrowseEmptyTitle,
        emptyMessage = strings.mkBrowseEmptyMessage,
        emptyIcon = Icons.Filled.Handshake,
        header = {
            if (!tipDismissed && state.collections.isNotEmpty()) {
                item(key = "browse_tip") {
                    BrowseTip(onDismiss = { DismissedHintsStore.dismiss(DismissedHintsStore.MARKETPLACE_BROWSE) })
                }
            }
        },
        content = {
            items(state.collections, key = { it.id }) { collection ->
                CollectionCard(
                    collection = collection,
                    showStatus = false,
                    onClick = { onOpenCollection(collection.id) },
                    showViewDrugsButton = true
                )
            }
            if (state.isLoadingMore) {
                item {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(28.dp))
                    }
                }
            }
        },
        onReachedEnd = viewModel::loadMore
    )
}

// First-visit hint; once closed it stays hidden on this device (DismissedHintsStore).
@Composable
private fun BrowseTip(onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PrimaryBlueContainer)
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.TouchApp, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            strings.mkBrowseTip,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp)
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = strings.commonDismiss, tint = TextSecondary)
        }
    }
}

@Composable
private fun OrdersTab(
    viewModel: MarketOrdersViewModel,
    asSeller: Boolean,
    emptyTitle: String,
    emptyMessage: String,
    onOpenOrder: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TabList(
        isLoading = state.isLoading || !state.loaded,
        error = state.error,
        isEmpty = state.orders.isEmpty(),
        onRetry = viewModel::refresh,
        emptyTitle = emptyTitle,
        emptyMessage = emptyMessage,
        emptyIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        content = {
            items(state.orders, key = { it.id }) { order ->
                MarketOrderCard(order = order, asSeller = asSeller, onClick = { onOpenOrder(order.id) })
            }
        }
    )
}

@Composable
private fun CollectionsTab(viewModel: MyCollectionsViewModel, onOpenCollection: (String) -> Unit) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdCollectionId) {
        state.createdCollectionId?.let {
            viewModel.onCreatedCollectionOpened()
            onOpenCollection(it)
        }
    }

    TabList(
        isLoading = state.isLoading || !state.loaded,
        error = state.error,
        isEmpty = state.collections.isEmpty(),
        onRetry = viewModel::refresh,
        emptyTitle = strings.mkCollectionsEmptyTitle,
        emptyMessage = strings.mkCollectionsEmptyMessage,
        emptyIcon = Icons.Filled.Inventory2,
        header = {
            item {
                PharmaButton(
                    text = strings.mkNewCollection,
                    onClick = viewModel::openCreate,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        content = {
            items(state.collections, key = { it.id }) { collection ->
                CollectionCard(collection = collection, showStatus = true, onClick = { onOpenCollection(collection.id) })
            }
        }
    )

    state.createForm?.let { form ->
        CreateCollectionDialog(
            form = form,
            onEdit = viewModel::editCreate,
            onSubmit = viewModel::submitCreate,
            onDismiss = viewModel::dismissCreate
        )
    }
}

@Composable
private fun InventoryTab(viewModel: MarketInventoryViewModel) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showToast = rememberToast(long = true)
    LaunchedEffect(state.message) {
        state.message?.let {
            showToast(it)
            viewModel.onMessageShown()
        }
    }
    val launchFilePicker = rememberFilePickerLauncher(
        mimeTypes = listOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "text/csv",
            "text/comma-separated-values"
        ),
        onResult = { uri, name -> viewModel.importFile(uri, name) }
    )

    TabList(
        isLoading = state.isLoading || !state.loaded,
        error = state.error,
        isEmpty = state.items.isEmpty(),
        onRetry = viewModel::refresh,
        emptyTitle = strings.mkInventoryEmptyTitle,
        emptyMessage = strings.mkInventoryEmptyMessage,
        emptyIcon = Icons.Filled.Medication,
        header = {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PharmaButton(
                        text = strings.mkAddDrug,
                        onClick = viewModel::openAdd,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = launchFilePicker,
                        enabled = !state.isImporting,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        if (state.isImporting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.UploadFile, null, Modifier.size(18.dp))
                            Text(" ${strings.mkImportExcel}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        },
        content = {
            items(state.items, key = { it.id }) { item -> InventoryRow(item) }
        }
    )

    state.addForm?.let { form ->
        AddInventoryDialog(
            form = form,
            onQueryChange = viewModel::onDrugQueryChange,
            onDrugSelected = viewModel::onDrugSelected,
            onEdit = viewModel::editAdd,
            onSubmit = viewModel::submitAdd,
            onDismiss = viewModel::dismissAdd
        )
    }
}
