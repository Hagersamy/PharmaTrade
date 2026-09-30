package com.pharmatrade.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

// Lives in the nav entry's ViewModelStore, so unlike the composition it survives the Activity
// being recreated — that's what lets the new composition know the previous one was torn down
// by a configuration change rather than a real leave.
internal class ResumeRefreshGate : ViewModel() {
    var skipNextResume = false
}

// Runs [onRefresh] on every ON_RESUME of this entry — including the synchronous replay when the
// observer attaches, which covers first load — EXCEPT the one that follows a configuration
// change (rotation, dark mode, locale, ...). The entry's ViewModels survive those with their data
// intact, so re-fetching just duplicated every request (one full batch per recreation).
@Composable
internal fun RefreshOnResume(onRefresh: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    val gate: ResumeRefreshGate = viewModel()
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> gate.skipNextResume = activity?.isChangingConfigurations == true
                Lifecycle.Event.ON_RESUME ->
                    if (gate.skipNextResume) gate.skipNextResume = false else currentOnRefresh()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
