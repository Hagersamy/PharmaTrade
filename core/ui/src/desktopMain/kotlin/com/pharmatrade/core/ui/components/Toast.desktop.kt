package com.pharmatrade.core.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun rememberToast(): (message: String) -> Unit = {}
