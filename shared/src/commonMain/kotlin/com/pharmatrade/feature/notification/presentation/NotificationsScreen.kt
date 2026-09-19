package com.pharmatrade.feature.notification.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color as ComposeColor
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.util.formatBackendTimestamp
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.LoadingScreen
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.theme.BackgroundGray
import com.pharmatrade.core.ui.theme.PrimaryBlue
import com.pharmatrade.core.ui.theme.PrimaryBlueContainer
import com.pharmatrade.core.ui.theme.SurfaceWhite
import com.pharmatrade.core.ui.theme.TextHint
import com.pharmatrade.core.ui.theme.TextPrimary
import com.pharmatrade.core.ui.theme.TextSecondary
import com.pharmatrade.feature.notification.domain.model.Notification

@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel,
    onNavigateBack: () -> Unit,
    onNotificationClick: (Notification) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val strings = LocalStrings.current

    // Re-fires every time this screen is navigated to (a fresh composition per nav-stack visit),
    // matching the backend's documented flow: fetch the list, then mark it all read.
    LaunchedEffect(Unit) { viewModel.openScreen() }

    // Infinite scroll: once the last visible row is within 3 of the end of what's loaded, fetch
    // the next page. Re-evaluates automatically as the list scrolls or grows, since it only reads
    // listState (snapshot-backed) and the current item count.
    val shouldLoadMore by remember(uiState.notifications.size) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= uiState.notifications.lastIndex - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(
            title = strings.notificationsTitle,
            onNavigateBack = onNavigateBack,
            actions = {
                if (uiState.notifications.isNotEmpty()) {
                    TextButton(onClick = viewModel::clearAll) {
                        Text(strings.commonClearAll, color = ComposeColor.White)
                    }
                }
            }
        )
        when {
            uiState.isLoading && uiState.notifications.isEmpty() -> LoadingScreen(modifier = Modifier.fillMaxSize())
            uiState.error != null && uiState.notifications.isEmpty() -> ErrorScreen(
                message = uiState.error.orEmpty(),
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxSize()
            )
            uiState.notifications.isEmpty() -> EmptyState(
                title = strings.notificationsEmptyTitle,
                message = strings.notificationsEmptyMessage,
                icon = Icons.Filled.Notifications
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = {
                            viewModel.markAsRead(notification.id)
                            onNotificationClick(notification)
                        },
                        onDelete = { viewModel.deleteNotification(notification.id) }
                    )
                }
                if (uiState.isLoadingMore) {
                    item(key = "loading_more") {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).wrapContentWidth(Alignment.CenterHorizontally)) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) PrimaryBlueContainer else SurfaceWhite
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!notification.isRead) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryBlue))
                }
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatBackendTimestamp(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = strings.notificationsDeleteContentDescription, tint = TextSecondary)
            }
        }
    }
}
