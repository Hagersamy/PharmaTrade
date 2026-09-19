package com.pharmatrade

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import android.util.Log
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.di.AppContainer
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class PharmaTradeApp : Application() {
    lateinit var container: AppContainer
        private set

    // Application has no built-in coroutine scope (unlike a ViewModel or Activity); this lives
    // for the process lifetime, matching the session-tracking observer below.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Must run before anything below can trigger a network call — picks the backend URL for
        // this build flavor (see app/build.gradle.kts productFlavors) instead of ApiClient's
        // built-in production default.
        ApiClient.baseUrl = BuildConfig.BASE_URL
        val prefs = getSharedPreferences("pharmatrade_session", MODE_PRIVATE)
        SessionManager.init(SharedPreferencesSettings(prefs))
        val languagePrefs = getSharedPreferences("pharmatrade_prefs", MODE_PRIVATE)
        LanguageManager.init(SharedPreferencesSettings(languagePrefs))
        container = AppContainer(this)
        setupAutoLogoutOnInvalidToken()
        setupCoil()
        setupCrashlyticsUserTracking()
        logFcmTokenForDebugging()
    }

    // Any API call that comes back 401 while we still think we're logged in means the backend
    // no longer honors this token (expired/revoked) — reuse the same logout flow the user's own
    // "Sign Out" button uses: call the logout endpoint (best-effort), then clear the local session.
    private fun setupAutoLogoutOnInvalidToken() {
        ApiClient.onUnauthorized = {
            appScope.launch { container.authRepository.logout() }
        }
    }

    // TEMP diagnostic: onNewToken only fires once (first token generation or rotation), so it logs
    // nothing on a run where the token was already established. This forces a fetch on every launch
    // so we can see the actual success/failure reason in logcat while debugging why no push
    // notification has ever reached this device.
    private fun logFcmTokenForDebugging() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> Log.i("PharmaFCM", "Current FCM token: $token") }
            .addOnFailureListener { e -> Log.e("PharmaFCM", "Failed to fetch FCM token", e) }
    }

    // Tags every crash/non-fatal report with which account hit it — without this, Crashlytics
    // reports are anonymous and much harder to correlate with a specific user's support ticket.
    private fun setupCrashlyticsUserTracking() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        SessionManager.currentUser
            .onEach { user ->
                crashlytics.setUserId(user?.id.orEmpty())
                crashlytics.setCustomKey("user_type", user?.userType?.name.orEmpty())
                crashlytics.setCustomKey("business_name", user?.businessName.orEmpty())
            }
            .launchIn(appScope)
    }

    private fun setupCoil() {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .apply {
                        SessionManager.authToken?.let {
                            addHeader("Authorization", "Bearer $it")
                        }
                    }
                    .build()
                chain.proceed(req)
            }
            .build()

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(OkHttpNetworkFetcherFactory(callFactory = { client }))
                }
                .build()
        }
    }
}
