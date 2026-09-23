package com.pharmatrade.core.network

import com.pharmatrade.core.common.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Ktor's default logger writes via println, which on some Android devices/ROMs never reaches
// logcat (println isn't guaranteed to be redirected there) — each platform provides a logger
// that reliably surfaces request/response bodies (e.g. Logger.ANDROID on Android, which uses
// android.util.Log directly), so network activity can actually be inspected during debugging.
internal expect val httpLogger: Logger

object ApiClient {

    // Overridden at app startup from the platform's own build config (e.g. Android's
    // BuildConfig.BASE_URL, set per product flavor) — see PharmaTradeApp.onCreate(). Defaults
    // to production so any platform/target that never sets it still hits the real backend.
    // Ktor's defaultRequest {} block below re-reads this on every request (same way it already
    // re-reads SessionManager.authToken), so setting it before the first network call is enough.
    var baseUrl: String = "https://pharma-trade-backend-production-e64c.up.railway.app/api/v1/"

    // Set once at app startup (see PharmaTradeApp.onCreate) to the app's own logout flow — kept
    // as a callback instead of a direct call so this core/network layer never has to depend on
    // the feature-layer AuthRepository. Only fires when a session token is actually set, so a
    // wrong-password attempt on the login screen (also a 401, but with no token yet) is unaffected.
    var onUnauthorized: (() -> Unit)? = null

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        isLenient = true
    }

    val httpClient: HttpClient = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            level = LogLevel.BODY
            logger = httpLogger
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 60_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
        HttpResponseValidator {
            // Not validateResponse: Ktor's own expectSuccess-driven validator always runs before any
            // validator this app adds (see HttpClient's plugin merge order + HttpCallValidator's
            // "last added executes first" reversal), so it throws ClientRequestException for every
            // non-2xx before this block would ever be reached. handleResponseExceptionWithRequest is
            // invoked from the pipeline's catch blocks instead, so it still runs after that exception.
            handleResponseExceptionWithRequest { cause, _ ->
                if (cause is ClientRequestException &&
                    cause.response.status == HttpStatusCode.Unauthorized &&
                    SessionManager.authToken != null
                ) {
                    onUnauthorized?.invoke()
                }
            }
        }
        defaultRequest {
            url(baseUrl)
            header("Accept", "application/json")
            header("ngrok-skip-browser-warning", "true")
            SessionManager.authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}
