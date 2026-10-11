package com.pharmatrade.core.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberToast(long: Boolean): (message: String) -> Unit {
    val context = LocalContext.current.applicationContext
    val duration = if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    return remember(context, duration) { { message -> Toast.makeText(context, message, duration).show() } }
}
