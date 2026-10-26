package com.pharmatrade.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pharmatrade.core.ui.theme.DividerGray
import com.pharmatrade.core.ui.theme.PrimaryBlue
import com.pharmatrade.core.ui.theme.PrimaryBlueContainer
import com.pharmatrade.core.ui.theme.SurfaceWhite

// A supplier's logo in a circle. Without a logo — or while it loads, or if it fails to — shows
// a generic avatar instead, so the slot never sits empty.
@Composable
fun SupplierAvatar(
    name: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    var logoLoaded by remember(logoUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (logoLoaded) SurfaceWhite else PrimaryBlueContainer)
            .border(1.dp, DividerGray, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!logoLoaded) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(size * 0.6f)
            )
        }
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                onSuccess = { logoLoaded = true },
                onError = { logoLoaded = false },
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
    }
}
