package com.pharmatrade.core.common.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private const val PREFS_NAME = "pharmatrade_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER = "auth_user"

    private val gson = Gson()
    private var prefs: SharedPreferences? = null

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

    // Must be called once from Application.onCreate() before any screen reads session state,
    // so a previously logged-in user is restored without hitting the login screen again.
    fun init(context: Context) {
        val store = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = store

        val storedUser = store.getString(KEY_USER, null)?.let { json ->
            runCatching { gson.fromJson(json, User::class.java) }.getOrNull()
        }

        if (storedUser != null) {
            authToken = store.getString(KEY_TOKEN, null)
            _currentUser.value = storedUser
            Log.d("SessionManager", "Restored session for ${storedUser.phone} — token: $authToken")
        } else {
            // Nothing to restore, or the saved user JSON was corrupt — clear any leftovers.
            store.edit().clear().apply()
        }
    }

    fun login(user: User, token: String? = null) {
        authToken = token
        _currentUser.value = user
        Log.d("SessionManager", "Logged in as ${user.phone} — token: $token")
        prefs?.edit()
            ?.putString(KEY_TOKEN, token)
            ?.putString(KEY_USER, gson.toJson(user))
            ?.apply()
    }

    fun logout() {
        authToken = null
        _currentUser.value = null
        prefs?.edit()?.clear()?.apply()
    }
}
