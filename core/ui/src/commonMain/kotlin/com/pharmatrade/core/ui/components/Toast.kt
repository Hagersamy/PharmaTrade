package com.pharmatrade.core.ui.components

import androidx.compose.runtime.Composable

// Returns a function that shows a short system toast (Android). Platforms without a toast
// concept get a no-op, so callers in shared UI code don't need to branch.
@Composable
expect fun rememberToast(): (message: String) -> Unit
