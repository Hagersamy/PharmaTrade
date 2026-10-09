package com.pharmatrade

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import android.util.Base64
import android.util.Log
import coil3.map.Mapper
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pharmatrade.core.common.log.CrashReportSink
import com.pharmatrade.core.common.log.CrashReporter
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
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

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
    private fun setupAutoLogoutOnInvalidToken() {
        ApiClient.onUnauthorized = {
            appScope.launch { container.authRepository.logout() }
        }
    }
    private fun logFcmTokenForDebugging() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> Log.i("PharmaFCM", "Current FCM token: $token") }
            .addOnFailureListener { e -> Log.e("PharmaFCM", "Failed to fetch FCM token", e) }
    }

    private fun setupCrashlyticsUserTracking() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        CrashReporter.sink = object : CrashReportSink {
            override fun log(message: String) = crashlytics.log(message)
            override fun setKey(key: String, value: String) = crashlytics.setCustomKey(key, value)
            override fun recordNonFatal(error: Throwable, keys: Map<String, String>) {
                crashlytics.recordException(error, CustomKeysAndValues.Builder().apply {
                    keys.forEach { (k, v) -> putString(k, v) }
                }.build())
            }
        }
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
                    add(DataUriMapper())
                    add(OkHttpNetworkFetcherFactory(callFactory = { client }))
                }
                .build()
        }
    }

    private class DataUriMapper : Mapper<String, ByteArray> {
        override fun map(data: String, options: Options): ByteArray? {
            if (!data.startsWith("data:")) return null
            val payload = data.substringAfter(",", missingDelimiterValue = "")
            return runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull()
        }
    }

    private fun unwrapJsonImageResponse(chain: Interceptor.Chain, response: Response): Response {
        val contentType = response.body?.contentType()
        if (!response.isSuccessful || contentType?.subtype?.contains("json") != true) return response
        val text = response.body?.string().orEmpty()
        val side = response.request.url.queryParameter("licence_side")
        val target = runCatching {
            val root = JSONTokener(text).nextValue()
            if (side != null) findLicenceBySide(root, side) else findImageRef(root)
        }.getOrNull()
        Log.i("LicenceImage", "JSON from ${response.request.url} -> ${target?.take(80)}")

        if (target == null && side != null) {
            return response.newBuilder().code(404).message("No $side licence image")
                .body(text.toResponseBody(contentType)).build()
        }
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
    private fun findLicenceBySide(node: Any?, side: String): String? {
        val images = findArray(node, "licence_images") ?: return null
        val wantPrimary = side == "front"
        return (0 until images.length())
            .mapNotNull { images.optJSONObject(it) }
            .firstOrNull { it.optBoolean("is_primary") == wantPrimary }
            ?.let { image ->
                image.optString("data_url").takeIf { it.startsWith("data:") }
                    ?: image.optString("url").takeIf { it.startsWith("http") }
            }
    }

    private fun findArray(node: Any?, key: String): JSONArray? = when (node) {
        is JSONObject -> node.optJSONArray(key)
            ?: node.keys().asSequence().firstNotNullOfOrNull { findArray(node.opt(it), key) }
        else -> null
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
