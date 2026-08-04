package com.pharmatrade.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

// Thin wrapper around the app's single back stack. The whole app is one flat stack (the bottom
// nav bar on Home is local Compose tab state, not a nav destination), so this deliberately skips
// the multi-top-level-route NavigationState machinery from the official migration guide — it
// doesn't apply here.
class Navigator(private val backStack: NavBackStack<NavKey>) {

    // Mirrors Nav2's navigate(route) { popUpTo(target) { inclusive = ... } }. target == null
    // mirrors Nav2's popUpTo(0) (the graph root sentinel) — pops the entire stack. Matching is by
    // structural equality (data class/object equals()), which is fine today since every real
    // popUpTo target is a parameterless data object (Login, Home, OrderMode) or null — if a
    // future call site needs to match "any instance of a parameterized key" (e.g. any OrderDetail
    // regardless of orderId), this will need to become a type check instead.
    data class PopUpTo(val target: NavKey?, val inclusive: Boolean = false)

    fun navigate(key: NavKey, popUpTo: PopUpTo? = null) {
        popUpTo?.let { applyPopUpTo(it) }
        backStack.add(key)
    }

    fun goBack() {
        backStack.removeLastOrNull()
    }

    private fun applyPopUpTo(popUpTo: PopUpTo) {
        if (popUpTo.target == null) {
            backStack.clear()
            return
        }
        val idx = backStack.indexOfLast { it == popUpTo.target }
        if (idx == -1) return
        val cutoff = if (popUpTo.inclusive) idx else idx + 1
        while (backStack.size > cutoff) backStack.removeAt(backStack.lastIndex)
    }
}
