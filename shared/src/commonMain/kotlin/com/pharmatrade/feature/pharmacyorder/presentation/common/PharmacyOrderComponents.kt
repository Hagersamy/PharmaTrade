package com.pharmatrade.feature.pharmacyorder.presentation.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmatrade.core.ui.theme.ErrorRed
import com.pharmatrade.core.ui.theme.ErrorRedContainer
import com.pharmatrade.core.ui.theme.PrimaryBlue
import com.pharmatrade.core.ui.theme.PrimaryBlueContainer
import com.pharmatrade.core.ui.theme.SecondaryGreen
import com.pharmatrade.core.ui.theme.SecondaryGreenContainer
import com.pharmatrade.core.ui.theme.SecondaryGreenDark
import com.pharmatrade.core.ui.theme.WarningAmber
import com.pharmatrade.core.ui.theme.WarningAmberContainer

// Maps a raw backend order status string to a (label, textColor, backgroundColor) triple.
fun orderStatusLook(status: String): Triple<String, Color, Color> = when (status.lowercase()) {
    "draft" -> Triple("Draft", WarningAmber, WarningAmberContainer)
    "pending_supplier_confirmation" -> Triple("Pending", PrimaryBlue, PrimaryBlueContainer)
    "confirmed" -> Triple("Confirmed", SecondaryGreenDark, SecondaryGreenContainer)
    "shipped" -> Triple("Shipped", PrimaryBlue, PrimaryBlueContainer)
    "delivered" -> Triple("Delivered", SecondaryGreen, SecondaryGreenContainer)
    "partially_available" -> Triple("Shortage", ErrorRed, ErrorRedContainer)
    "cancelled" -> Triple("Cancelled", ErrorRed, ErrorRedContainer)
    else -> Triple(status.replaceFirstChar { it.uppercase() }, WarningAmber, WarningAmberContainer)
}

@Composable
fun OrderStatusChip(status: String, modifier: Modifier = Modifier) {
    val (label, color, bg) = orderStatusLook(status)
    Surface(shape = RoundedCornerShape(20.dp), color = bg, modifier = modifier) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
