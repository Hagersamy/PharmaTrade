package com.pharmatrade.core.common.session

import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SessionManager {
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER = "auth_user"

    private var settings: Settings? = null

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    var authToken: String? = null
        private set

    val user: User? get() = _currentUser.value
    val isLoggedIn: Boolean get() = _currentUser.value != null
    val isSeller: Boolean get() = _currentUser.value?.userType == UserType.SELLER
    val isBuyer: Boolean get() = _currentUser.value?.userType == UserType.BUYER
    val isAdmin: Boolean get() = _currentUser.value?.userType == UserType.ADMIN
    val isPending: Boolean get() = _currentUser.value?.isPending == true

    // Must be called once from each platform's app entry point (with a platform-backed
    // Settings instance) before any screen reads session state, so a previously logged-in
    // user is restored without hitting the login screen again.
    fun init(settings: Settings) {
        this.settings = settings

        val storedUser = settings.getStringOrNull(KEY_USER)?.let { json ->
            runCatching { Json.decodeFromString<User>(json) }.getOrNull()
        }

        if (storedUser != null) {
            authToken = settings.getStringOrNull(KEY_TOKEN)
            _currentUser.value = storedUser
        } else {
            // Nothing to restore, or the saved user JSON was corrupt — clear any leftovers.
            settings.remove(KEY_TOKEN)
            settings.remove(KEY_USER)
        }
    }

    fun login(user: User, token: String? = null) {
        authToken = token
        _currentUser.value = user
        settings?.putString(KEY_USER, Json.encodeToString(user))
        if (token != null) settings?.putString(KEY_TOKEN, token) else settings?.remove(KEY_TOKEN)
    }

    fun logout() {
        authToken = null
        _currentUser.value = null
        settings?.remove(KEY_TOKEN)
        settings?.remove(KEY_USER)
    }
}
