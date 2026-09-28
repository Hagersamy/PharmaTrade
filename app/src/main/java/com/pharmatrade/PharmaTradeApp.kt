package com.pharmatrade

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import android.util.Base64
import android.util.Log
import coil3.map.Mapper
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.reminder.UploadReminderStore
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.di.AppContainer
import com.pharmatrade.push.AndroidUploadReminderScheduler
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

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
        val reminderPrefs = getSharedPreferences("pharmatrade_reminders", MODE_PRIVATE)
        UploadReminderStore.init(SharedPreferencesSettings(reminderPrefs))
        UploadReminderStore.startSync(appScope, AndroidUploadReminderScheduler(this))
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
                unwrapJsonImageResponse(chain, chain.proceed(req))
            }
            .build()

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    // Must come before Coil's own String→Uri mapping, which has no data: URI support.
                    add(DataUriMapper())
                    add(OkHttpNetworkFetcherFactory(callFactory = { client }))
                }
                .build()
        }
    }

    // Admin licence images come as "data:image/jpeg;base64,…" (registration-requests data_url).
    // Coil 3 can't fetch data: URIs, but it can decode raw bytes.
    private class DataUriMapper : Mapper<String, ByteArray> {
        override fun map(data: String, options: Options): ByteArray? {
            if (!data.startsWith("data:")) return null
            val payload = data.substringAfter(",", missingDelimiterValue = "")
            return runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull()
        }
    }

    // Endpoints like admin/registration-requests/{id}/licence-image-url may answer with JSON that
    // points at the image (a signed/storage url or an inline data_url) instead of the image bytes.
    // Coil can't decode JSON, so follow that pointer here and hand Coil the actual image.
    private fun unwrapJsonImageResponse(chain: Interceptor.Chain, response: Response): Response {
        val contentType = response.body?.contentType()
        if (!response.isSuccessful || contentType?.subtype?.contains("json") != true) return response
        val text = response.body?.string().orEmpty()
        val target = runCatching { findImageRef(JSONTokener(text).nextValue()) }.getOrNull()
        Log.i("LicenceImage", "JSON from ${response.request.url} -> ${target?.take(80)}")

        if (target == null) {
            return response.newBuilder().body(text.toResponseBody(contentType)).build()
        }
        if (target.startsWith("data:")) {
            val mime = target.substringAfter("data:").substringBefore(";").ifBlank { "image/jpeg" }
            val bytes = Base64.decode(target.substringAfter(","), Base64.DEFAULT)
            return response.newBuilder().body(bytes.toResponseBody(mime.toMediaTypeOrNull())).build()
        }

        // The backend builds links from its APP_URL, which may be http:// or localhost — keep them
        // on the host we actually reached, over https, and only send our token to that host.
        val original = response.request.url
        val parsed = target.toHttpUrlOrNull() ?: return response.newBuilder().body(text.toResponseBody(contentType)).build()
        val sameBackend = parsed.host == original.host || parsed.host in setOf("localhost", "127.0.0.1", "10.0.2.2")
        val followUrl = if (sameBackend) parsed.newBuilder().scheme(original.scheme).host(original.host).port(original.port).build() else parsed
        val followReq = Request.Builder()
            .url(followUrl)
            .header("ngrok-skip-browser-warning", "true")
            .apply { if (sameBackend) SessionManager.authToken?.let { header("Authorization", "Bearer $it") } }
            .build()
        return chain.proceed(followReq)
    }

    private fun findImageRef(node: Any?): String? = when (node) {
        is JSONObject -> {
            listOf("data_url", "url", "signed_url", "temporary_url", "image_url", "path")
                .firstNotNullOfOrNull { key ->
                    node.optString(key).takeIf { it.startsWith("data:") || it.startsWith("http") }
                }
                ?: node.keys().asSequence().firstNotNullOfOrNull { findImageRef(node.opt(it)) }
        }
        is JSONArray -> (0 until node.length()).firstNotNullOfOrNull { findImageRef(node.opt(it)) }
        else -> null
    }
}
