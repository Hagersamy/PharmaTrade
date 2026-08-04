package com.pharmatrade.core.network

import com.pharmatrade.core.common.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Ktor's default logger writes via println, which on some Android devices/ROMs never reaches
// logcat (println isn't guaranteed to be redirected there) — each platform provides a logger
// that reliably surfaces request/response bodies (e.g. Logger.ANDROID on Android, which uses
// android.util.Log directly), so network activity can actually be inspected during debugging.
internal expect val httpLogger: Logger

object ApiClient {

    private const val BASE_URL = "https://unbuckled-word-defuse.ngrok-free.dev/api/v1/"

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
        defaultRequest {
            url(BASE_URL)
            header("Accept", "application/json")
            header("ngrok-skip-browser-warning", "true")
            SessionManager.authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}
