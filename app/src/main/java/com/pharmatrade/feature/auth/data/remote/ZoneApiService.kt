package com.pharmatrade.feature.auth.data.remote

import com.google.gson.JsonElement
import retrofit2.http.GET

interface ZoneApiService {
    @GET("zones")
    suspend fun getZones(): JsonElement
}
