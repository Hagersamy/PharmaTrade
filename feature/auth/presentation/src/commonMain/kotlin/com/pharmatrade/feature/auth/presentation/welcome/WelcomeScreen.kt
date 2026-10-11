package com.pharmatrade.feature.auth.presentation.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.ui.theme.*

// First screen for a logged-out user: the logo plus the two ways in.
@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    // The platform's app icon, supplied by the app module (see LoginScreen's appLogo).
    appLogo: Painter? = null
) {
    val strings = LocalStrings.current
    // Light: logo-blue background with white buttons. Dark: the app's dark background, where the
    // blue logo stands out on its own, with blue buttons instead of white.
    val isDark = LocalAppColors.current == DarkAppColors
    val background = if (isDark) BackgroundGray else BrandBlue
    val taglineColor = if (isDark) TextSecondary else Color.White.copy(alpha = 0.9f)
    val primaryContainer = if (isDark) PrimaryBlue else Color.White
    val primaryContent = if (isDark) Color.White else BrandBlue
    val secondaryColor = if (isDark) PrimaryBlueLight else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .systemBarsPadding()
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (appLogo != null) {
                Image(
                    painter = appLogo,
                    contentDescription = strings.appTitle,
                    modifier = Modifier.size(140.dp).clip(CircleShape)
                )
                Spacer(Modifier.height(24.dp))
            }
            Text(
                text = strings.loginTagline,
                style = MaterialTheme.typography.titleMedium,
                color = taglineColor,
                textAlign = TextAlign.Center
            )
        }

        // Sign-up first as the main (filled) action, then sign-in; both fully rounded pills.
        Button(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = primaryContainer, contentColor = primaryContent)
        ) {
            Text(strings.regCreateAccount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = CircleShape,
            border = BorderStroke(1.5.dp, secondaryColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = secondaryColor)
        ) {
            Text(strings.loginSignIn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
