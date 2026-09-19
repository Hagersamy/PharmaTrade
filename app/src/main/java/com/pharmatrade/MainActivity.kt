package com.pharmatrade

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.pharmatrade.core.ui.theme.PharmaTradeTheme
import com.pharmatrade.navigation.AppNavigation
import com.pharmatrade.push.NotificationDeepLink
import com.pharmatrade.push.toNotificationDeepLink

class MainActivity : ComponentActivity() {
    // Set from the launching Intent's extras (cold start) or onNewIntent (already running, e.g.
    // launchMode="singleTop" reusing this instance) when it came from tapping a push notification.
    // AppNavigation observes this and navigates once, then clears it.
    private val pendingDeepLink = mutableStateOf<NotificationDeepLink?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or not, nothing else to do here */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PharmaTradeApp).container
        pendingDeepLink.value = intent.toNotificationDeepLink()
        requestNotificationPermissionIfNeeded()
        setContent {
            PharmaTradeTheme {
                AppNavigation(container = container, pendingDeepLink = pendingDeepLink)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.value = intent.toNotificationDeepLink()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
