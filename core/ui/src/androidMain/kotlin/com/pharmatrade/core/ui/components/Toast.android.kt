package com.pharmatrade.core.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberToast(): (message: String) -> Unit {
    val context = LocalContext.current.applicationContext
    return remember(context) { { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() } }
}
