package com.pharmatrade.feature.auth.data.repository

import com.google.gson.JsonElement
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.network.RetrofitClient
import com.pharmatrade.feature.auth.data.remote.ZoneApiService
import com.pharmatrade.feature.auth.domain.model.Zone
import com.pharmatrade.feature.auth.domain.repository.ZoneRepository
import retrofit2.HttpException

class ZoneRepositoryImpl : ZoneRepository {

    private val api = RetrofitClient.create<ZoneApiService>()

    override suspend fun getZones(): Result<List<Zone>> {
        return try {
            val zones = parseZones(api.getZones())
            if (zones.isEmpty()) Result.Error("No zones available")
            else Result.Success(zones)
        } catch (e: HttpException) {
            Result.Error("Server error (${e.code()})")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load zones")
        }
    }

    // Actual response shape:
    // { "data": { "total": 7, "grouped": [ { "governorate": "Cairo", "zones": [ {id, name, is_active} ] } ] } }
    private fun parseZones(element: JsonElement): List<Zone> {
        if (!element.isJsonObject) return emptyList()

        val data = element.asJsonObject.get("data")
            ?.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()

        val grouped = data.get("grouped")
            ?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()

        val zones = mutableListOf<Zone>()
        for (groupEl in grouped) {
            if (!groupEl.isJsonObject) continue
            val group = groupEl.asJsonObject
            val governorate = group.get("governorate")?.asString
            val zonesArr = group.get("zones")
                ?.takeIf { it.isJsonArray }?.asJsonArray ?: continue

            for (zoneEl in zonesArr) {
                if (!zoneEl.isJsonObject) continue
                val z = zoneEl.asJsonObject
                val id = z.get("id")?.asInt ?: continue
                val name = z.get("name")?.asString?.takeIf { it.isNotBlank() } ?: continue
                val isActive = z.get("is_active")?.asBoolean ?: true
                if (isActive) zones.add(Zone(id = id, name = name, governorate = governorate))
            }
        }
        return zones
    }
}
