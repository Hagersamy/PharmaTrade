package com.pharmatrade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pharmatrade.core.ui.theme.PharmaTradeTheme
import com.pharmatrade.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PharmaTradeApp).container
        setContent {
            PharmaTradeTheme {
                AppNavigation(container = container)
            }
        }
    }
}
