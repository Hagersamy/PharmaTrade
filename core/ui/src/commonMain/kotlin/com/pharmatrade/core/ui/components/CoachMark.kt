package com.pharmatrade.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmatrade.core.ui.theme.*

/** One stop of a guided tour: the element tagged with [key] gets highlighted and explained. */
class CoachMarkStep(val key: String, val title: String, val body: String)

/** Collects the on-screen bounds of every element tagged with [coachMarkTarget]. */
@Stable
class CoachMarkState {
    internal val targets = mutableStateMapOf<String, Rect>()
}

@Composable
fun rememberCoachMarkState(): CoachMarkState = remember { CoachMarkState() }

fun Modifier.coachMarkTarget(state: CoachMarkState, key: String): Modifier =
    onGloballyPositioned { state.targets[key] = it.boundsInRoot() }

/**
 * Guided tour over the real UI: dims the screen, cuts a spotlight around the current step's
 * element and shows a bubble explaining it, with Skip / Next. Place it last in a full-screen Box
 * so it draws on top and swallows touches meant for the screen underneath. Steps whose element
 * isn't on screen are skipped. [onFinish] fires on Skip or after the last step.
 */
@Composable
fun CoachMarkOverlay(
    state: CoachMarkState,
    steps: List<CoachMarkStep>,
    skipLabel: String,
    nextLabel: String,
    doneLabel: String,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSteps = steps.filter { it.key in state.targets }
    var index by remember { mutableIntStateOf(0) }
    val step = activeSteps.getOrNull(index) ?: return
    val target = state.targets.getValue(step.key)
    val isLast = index == activeSteps.lastIndex

    // Element bounds are root-relative; shift them into this overlay's own coordinates.
    var origin by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    val padding = with(density) { 6.dp.toPx() }
    val hole = origin?.let { target.translate(-it).inflate(padding) }

    // The spotlight glides from one element to the next instead of jumping.
    val animatedHole = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    var holePlaced by remember { mutableStateOf(false) }
    LaunchedEffect(hole) {
        if (hole == null) return@LaunchedEffect
        if (holePlaced) animatedHole.animateTo(hole) else animatedHole.snapTo(hole)
        holePlaced = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        if (!holePlaced) return@Box
        val spot = animatedHole.value
        val scrimColor = Color.Black.copy(alpha = 0.72f)
        val ringColor = Color.White.copy(alpha = 0.9f)
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            val radius = CornerRadius(minOf(spot.height, spot.width) / 2f)
            drawRect(scrimColor)
            drawRoundRect(Color.Transparent, spot.topLeft, spot.size, radius, blendMode = BlendMode.Clear)
            drawRoundRect(ringColor, spot.topLeft, spot.size, radius, style = Stroke(2.dp.toPx()))
        }

        // Bubble goes under the element when it fits, otherwise above; centred on it horizontally
        // but kept inside the screen. place() (not placeRelative) because the bounds are already
        // absolute, so RTL needs no mirroring here.
        Layout(
            content = {
                CoachMarkBubble(
                    step = step,
                    counter = "${index + 1} / ${activeSteps.size}",
                    isLast = isLast,
                    skipLabel = skipLabel,
                    nextLabel = nextLabel,
                    doneLabel = doneLabel,
                    onSkip = onFinish,
                    onNext = { if (isLast) onFinish() else index++ }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { measurables, constraints ->
            val margin = 16.dp.roundToPx()
            val gap = 12.dp.roundToPx()
            val maxWidth = minOf(constraints.maxWidth - margin * 2, 380.dp.roundToPx())
            val bubble = measurables.first().measure(
                constraints.copy(minWidth = 0, minHeight = 0, maxWidth = maxWidth.coerceAtLeast(0))
            )
            val below = spot.bottom.toInt() + gap
            val y = if (below + bubble.height + margin <= constraints.maxHeight) below
            else (spot.top.toInt() - gap - bubble.height).coerceAtLeast(margin)
            val x = (spot.center.x - bubble.width / 2f).toInt()
                .coerceIn(margin, (constraints.maxWidth - margin - bubble.width).coerceAtLeast(margin))
            layout(constraints.maxWidth, constraints.maxHeight) { bubble.place(x, y) }
        }
    }
}

@Composable
private fun CoachMarkBubble(
    step: CoachMarkStep,
    counter: String,
    isLast: Boolean,
    skipLabel: String,
    nextLabel: String,
    doneLabel: String,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(start = 18.dp, top = 16.dp, end = 12.dp, bottom = 8.dp)) {
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { shown ->
                Column(Modifier.padding(end = 6.dp)) {
                    Text(
                        shown.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(shown.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(counter, style = MaterialTheme.typography.labelMedium, color = TextHint)
                Spacer(Modifier.weight(1f))
                if (!isLast) {
                    TextButton(onClick = onSkip) { Text(skipLabel, color = TextSecondary) }
                }
                Button(
                    onClick = onNext,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White)
                ) {
                    Text(if (isLast) doneLabel else nextLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
