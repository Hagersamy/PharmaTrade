package com.pharmatrade.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pharmatrade.core.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Height the floating [LiquidGlassBottomBar] covers at the bottom of the screen (including the
 * system navigation bar). Content now scrolls *behind* the glass, so scrollable screens add this
 * to their bottom content padding to keep the last item reachable above the bar.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

data class GlassBarItem(
    val icon: ImageVector,
    val label: String,
    val badgeCount: Int = 0
)

/**
 * Floating "liquid glass" tab bar: a capsule that blurs the content scrolling behind it (the
 * content must be marked with `Modifier.haze(hazeState)`), with a lens-like pill that springs
 * between tabs. Colors come from the app palette, so it follows light/dark mode.
 */
@Composable
fun LiquidGlassBottomBar(
    items: List<GlassBarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppColors.current == DarkAppColors
    // Specular highlight strength: bright edge on light glass, a faint rim on dark glass.
    val rimTop = Color.White.copy(alpha = if (isDark) 0.22f else 0.85f)
    val rimBottom = Color.White.copy(alpha = if (isDark) 0.04f else 0.25f)
    val sheenTop = Color.White.copy(alpha = if (isDark) 0.08f else 0.35f)
    val barShape = CircleShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = barShape,
                    ambientColor = PrimaryBlue.copy(alpha = 0.18f),
                    spotColor = PrimaryBlue.copy(alpha = 0.22f)
                )
                .clip(barShape)
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = BackgroundGray,
                        tint = HazeTint(SurfaceWhite.copy(alpha = if (isDark) 0.55f else 0.5f)),
                        blurRadius = 28.dp,
                        noiseFactor = 0.08f,
                        // Pre-Android 12 has no backdrop blur: fall back to a near-opaque frost.
                        fallbackTint = HazeTint(SurfaceWhite.copy(alpha = 0.92f))
                    )
                )
                .background(Brush.verticalGradient(listOf(sheenTop, Color.Transparent)))
                .border(1.dp, Brush.verticalGradient(listOf(rimTop, rimBottom)), barShape)
                .padding(6.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val itemWidth = maxWidth / items.size.coerceAtLeast(1)
                GlassLens(
                    offset = itemWidth * selectedIndex,
                    width = itemWidth,
                    isDark = isDark
                )
                Row(modifier = Modifier.fillMaxSize()) {
                    items.forEachIndexed { index, item ->
                        GlassBarTab(
                            item = item,
                            selected = index == selectedIndex,
                            isDark = isDark,
                            onClick = { onItemSelected(index) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

/** The selection pill. Slides with a slightly under-damped spring for the "liquid" overshoot. */
@Composable
private fun GlassLens(offset: Dp, width: Dp, isDark: Boolean) {
    val animatedOffset by animateDpAsState(
        targetValue = offset,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "lensOffset"
    )
    Box(
        modifier = Modifier
            .offset(x = animatedOffset)
            .width(width)
            .fillMaxHeight()
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        PrimaryBlueContainer.copy(alpha = if (isDark) 0.95f else 0.9f),
                        PrimaryBlueContainer.copy(alpha = if (isDark) 0.7f else 0.65f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.18f else 0.9f),
                        PrimaryBlueLight.copy(alpha = 0.25f)
                    )
                ),
                CircleShape
            )
    )
}

@Composable
private fun GlassBarTab(
    item: GlassBarItem,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "tabScale"
    )
    // PrimaryBlue is too dark to read on the dark-mode container; use the lighter accent there.
    val selectedColor = if (isDark) PrimaryBlueLight else PrimaryBlue
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedColor else TextSecondary,
        label = "tabColor"
    )

    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BadgedBox(
            badge = {
                if (item.badgeCount > 0) Badge { Text("${item.badgeCount}") }
            }
        ) {
            Icon(item.icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
