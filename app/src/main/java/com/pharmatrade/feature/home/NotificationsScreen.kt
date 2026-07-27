package com.pharmatrade.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.theme.BackgroundGray

@Composable
fun NotificationsScreen(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(title = "Notifications", onNavigateBack = onNavigateBack)
        EmptyState(
            title = "No Notifications",
            message = "You'll receive updates about your orders and account here",
            icon = Icons.Filled.Notifications
        )
    }
}
