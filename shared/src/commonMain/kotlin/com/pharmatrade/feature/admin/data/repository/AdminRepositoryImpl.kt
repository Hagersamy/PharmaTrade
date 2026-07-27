package com.pharmatrade.feature.admin.data.repository

import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.data.remote.AdminApi
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.model.RegistrationStats
import com.pharmatrade.feature.admin.domain.repository.AdminRepository
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AdminRepositoryImpl(
    private val api: AdminApi = AdminApi()
) : AdminRepository {

    override suspend fun getPendingPharmacies(): Result<List<PendingUser>> = try {
        val response = api.getPendingPharmacies()
        Result.Success(response.data?.map { it.toPendingUser(UserType.BUYER) } ?: emptyList())
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load pharmacies")
    }

    override suspend fun getPendingSuppliers(): Result<List<PendingUser>> = try {
        val response = api.getPendingSuppliers()
        Result.Success(response.data?.map { it.toPendingUser(UserType.SELLER) } ?: emptyList())
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load suppliers")
    }

    override suspend fun approveUser(id: String, userType: UserType): Result<Unit> = try {
        if (userType == UserType.BUYER) api.approvePharmacy(id) else api.approveSupplier(id)
        Result.Success(Unit)
    } catch (e: ResponseException) {
        Result.Error("Failed to approve (${e.response.status.value})")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    override suspend fun rejectUser(id: String, userType: UserType): Result<Unit> = try {
        if (userType == UserType.BUYER) api.rejectPharmacy(id) else api.rejectSupplier(id)
        Result.Success(Unit)
    } catch (e: ResponseException) {
        Result.Error("Failed to reject (${e.response.status.value})")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    override suspend fun getRegistrationStats(): Result<RegistrationStats> = try {
        val response = api.getRegistrationStats()
        Result.Success(response.data?.toStats() ?: RegistrationStats())
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load stats")
    }

    override suspend fun getRegistrationRequests(entityType: String?): Result<List<PendingUser>> = try {
        val response = api.getRegistrationRequests(entityType = entityType)
        Result.Success(response.data?.items?.map { it.toPendingUser() } ?: emptyList())
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e))
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load requests")
    }

    override suspend fun approveRequest(id: String, notes: String): Result<Unit> = try {
        api.approveRequest(id, notes)
        Result.Success(Unit)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e))
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    override suspend fun declineRequest(id: String, reason: String): Result<Unit> = try {
        api.declineRequest(id, reason)
        Result.Success(Unit)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e))
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    private suspend fun parseHttpError(e: ResponseException): String {
        val code = e.response.status.value
        return try {
            val raw = runCatching { e.response.bodyAsText() }.getOrNull() ?: return "Server error ($code)"
            val json = Json.parseToJsonElement(raw).jsonObject
            val errors = json["errors"] as? JsonObject
            if (errors != null && errors.isNotEmpty()) {
                errors.values.firstOrNull()?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                    ?: json["message"]?.jsonPrimitive?.contentOrNull
                    ?: "Server error ($code)"
            } else {
                json["message"]?.jsonPrimitive?.contentOrNull ?: "Server error ($code)"
            }
        } catch (_: Exception) {
            "Server error ($code)"
        }
    }
}
