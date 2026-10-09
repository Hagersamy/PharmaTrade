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
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal expect val httpLogger: Logger

fun logNetwork(message: String) = httpLogger.log(message)

object ApiClient {

    var baseUrl: String = "https://pharma-trade-backend-production-e64c.up.railway.app/api/v1/"

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
