package com.pharmatrade.feature.auth.presentation.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.ui.theme.*

// The backend's own outcome for a registration request — used both for the initial
// post-registration wait screen and, once a decision has been made, to show the result when the
// user is routed here again (from a login attempt or a "registration_declined"/"registration_approved"
// notification tap).
enum class RegistrationOutcome { PENDING, DECLINED }

@Composable
fun PendingApprovalScreen(
    userName: String = "",
    outcome: RegistrationOutcome = RegistrationOutcome.PENDING,
    onBackToLogin: () -> Unit
) {
    val isDeclined = outcome == RegistrationOutcome.DECLINED
    val strings = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDeclined) listOf(ErrorRed, ErrorRedContainer) else listOf(PrimaryBlue, PrimaryBlueDark),
                    endY = 500f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDeclined) Icons.Filled.Close else Icons.Filled.HourglassTop,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = if (isDeclined) strings.declinedTitle else strings.pendingSubmittedTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            if (userName.isNotBlank()) {
                Text(
                    text = strings.pendingGreeting(userName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
            }

            Text(
                text = if (isDeclined) strings.declinedBodyMessage else strings.pendingBodyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // Info card — only relevant while still waiting on a decision.
            if (!isDeclined) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.pendingWhatsNextTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.pendingWhatsNextSteps,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PrimaryBlue
                )
            ) {
                Text(
                    text = strings.backToLogin,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
