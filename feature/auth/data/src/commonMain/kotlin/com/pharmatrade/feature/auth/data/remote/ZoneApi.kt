package com.pharmatrade.feature.auth.data.remote

import com.pharmatrade.core.network.ApiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonElement

class ZoneApi(private val client: HttpClient = ApiClient.httpClient) {
    suspend fun getZones(): JsonElement = client.get("zones").body()
}
