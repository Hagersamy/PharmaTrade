package com.pharmatrade.core.ui.components

import androidx.compose.runtime.Composable

// Returns a function that shows a system toast (short, or long with `long = true`) (Android). Platforms without a toast
// concept get a no-op, so callers in shared UI code don't need to branch.
@Composable
expect fun rememberToast(long: Boolean = false): (message: String) -> Unit
