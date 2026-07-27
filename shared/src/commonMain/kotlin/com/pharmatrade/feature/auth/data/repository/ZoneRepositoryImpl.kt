package com.pharmatrade.feature.auth.data.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.auth.data.remote.ZoneApi
import com.pharmatrade.feature.auth.domain.model.Zone
import com.pharmatrade.feature.auth.domain.repository.ZoneRepository
import io.ktor.client.plugins.ResponseException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

class ZoneRepositoryImpl(private val api: ZoneApi = ZoneApi()) : ZoneRepository {

    override suspend fun getZones(): Result<List<Zone>> {
        return try {
            val zones = parseZones(api.getZones())
            if (zones.isEmpty()) Result.Error("No zones available")
            else Result.Success(zones)
        } catch (e: ResponseException) {
            Result.Error("Server error (${e.response.status.value})")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load zones")
        }
    }

    // Actual response shape:
    // { "data": { "total": 7, "grouped": [ { "governorate": "Cairo", "zones": [ {id, name, is_active} ] } ] } }
    private fun parseZones(element: JsonElement): List<Zone> {
        val root = element as? JsonObject ?: return emptyList()
        val data = root["data"] as? JsonObject ?: return emptyList()
        val grouped = data["grouped"] as? JsonArray ?: return emptyList()

        val zones = mutableListOf<Zone>()
        for (groupEl in grouped) {
            val group = groupEl as? JsonObject ?: continue
            val governorate = group["governorate"].stringOrNull()
            val zonesArr = group["zones"] as? JsonArray ?: continue

            for (zoneEl in zonesArr) {
                val z = zoneEl as? JsonObject ?: continue
                val id = (z["id"] as? JsonPrimitive)?.intOrNull ?: continue
                val name = z["name"].stringOrNull()?.takeIf { it.isNotBlank() } ?: continue
                val isActive = (z["is_active"] as? JsonPrimitive)?.booleanOrNull ?: true
                if (isActive) zones.add(Zone(id = id, name = name, governorate = governorate))
            }
        }
        return zones
    }

    private fun JsonElement?.stringOrNull(): String? =
        (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
}
