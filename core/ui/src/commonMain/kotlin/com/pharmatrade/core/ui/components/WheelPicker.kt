package com.pharmatrade.core.ui.components

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pharmatrade.core.ui.theme.PrimaryBlue
import com.pharmatrade.core.ui.theme.TextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

// A scrollable, snapping "wheel" for picking one value out of a short list (hours, minutes,
// AM/PM). The centred row is the selection; onSelected fires whenever it changes.
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    itemHeight: Dp = 44.dp,
    visibleCount: Int = 5
) {
    // Blank rows above/below let the first and last real items reach the centre. With those in
    // place, the item snapped to the top slot is exactly the data index shown in the centre.
    val padCount = visibleCount / 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex.coerceIn(items.indices))
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val currentOnSelected by rememberUpdatedState(onSelected)

    val centredIndex by remember {
        derivedStateOf {
            val offsetSteps = if (listState.firstVisibleItemScrollOffset > itemHeightPx / 2) 1 else 0
            (listState.firstVisibleItemIndex + offsetSteps).coerceIn(items.indices)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centredIndex }.distinctUntilChanged().collect { currentOnSelected(it) }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(width).height(itemHeight * visibleCount)
    ) {
        items(padCount) { Box(Modifier.height(itemHeight)) }
        items(items.size) { index ->
            val isCentre = index == centredIndex
            Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                Text(
                    text = items[index],
                    style = if (isCentre) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCentre) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCentre) PrimaryBlue else TextSecondary,
                    modifier = Modifier.alpha(if (isCentre) 1f else 0.6f)
                )
            }
        }
        items(padCount) { Box(Modifier.height(itemHeight)) }
    }
}
