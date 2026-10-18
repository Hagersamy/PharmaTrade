package com.pharmatrade.feature.auth.presentation.intro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.ui.theme.*
import kotlinx.coroutines.launch

private class IntroPage(val icon: ImageVector, val title: String, val body: String)

// Swipeable slides shown once on first launch, before Welcome, explaining what the app is for.
// Same palette as WelcomeScreen so the two read as one flow. The pager follows the layout
// direction, so in Arabic the slides advance right-to-left on their own.
@Composable
fun IntroScreen(onFinish: () -> Unit) {
    val strings = LocalStrings.current
    val pages = listOf(
        IntroPage(Icons.Filled.LocalPharmacy, strings.introTitle1, strings.introBody1),
        IntroPage(Icons.Filled.Percent, strings.introTitle2, strings.introBody2),
        IntroPage(Icons.Filled.TrackChanges, strings.introTitle3, strings.introBody3),
        IntroPage(Icons.Filled.Handshake, strings.introTitle4, strings.introBody4)
    )
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    val isDark = LocalAppColors.current == DarkAppColors
    val background = if (isDark) BackgroundGray else BrandBlue
    val titleColor = if (isDark) TextPrimary else Color.White
    val bodyColor = if (isDark) TextSecondary else Color.White.copy(alpha = 0.9f)
    val accent = if (isDark) PrimaryBlueLight else Color.White
    val primaryContainer = if (isDark) PrimaryBlue else Color.White
    val primaryContent = if (isDark) Color.White else BrandBlue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .systemBarsPadding()
            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kept in place on the last page (just invisible) so the layout doesn't jump.
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
            if (!isLastPage) {
                TextButton(onClick = onFinish) {
                    Text(strings.introSkip, color = accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(accent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(page.icon, contentDescription = null, tint = accent, modifier = Modifier.size(68.dp))
                }
                Spacer(Modifier.height(40.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = bodyColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier.padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pages.size) { index ->
                val selected = index == pagerState.currentPage
                val width by animateDpAsState(if (selected) 24.dp else 8.dp)
                val color by animateColorAsState(if (selected) accent else accent.copy(alpha = 0.35f))
                Box(Modifier.height(8.dp).width(width).background(color, CircleShape))
            }
        }

        Button(
            onClick = {
                if (isLastPage) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = primaryContainer, contentColor = primaryContent)
        ) {
            Text(
                if (isLastPage) strings.introGetStarted else strings.introNext,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
