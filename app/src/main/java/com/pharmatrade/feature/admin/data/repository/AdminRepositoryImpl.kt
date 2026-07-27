package com.pharmatrade.feature.admin.data.repository

import com.google.gson.Gson
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.network.RetrofitClient
import com.pharmatrade.feature.admin.data.remote.AdminApiService
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.model.RegistrationStats
import com.pharmatrade.feature.admin.domain.repository.AdminRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class AdminRepositoryImpl : AdminRepository {

    private val api = RetrofitClient.create<AdminApiService>()

    override suspend fun getPendingPharmacies(): Result<List<PendingUser>> = try {
        val response = api.getPendingPharmacies()
        Result.Success(response.data?.map { it.toPendingUser(UserType.BUYER) } ?: emptyList())
    } catch (e: HttpException) {
        Result.Error("Server error (${e.code()})")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load pharmacies")
    }

    override suspend fun getPendingSuppliers(): Result<List<PendingUser>> = try {
        val response = api.getPendingSuppliers()
        Result.Success(response.data?.map { it.toPendingUser(UserType.SELLER) } ?: emptyList())
    } catch (e: HttpException) {
        Result.Error("Server error (${e.code()})")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load suppliers")
    }

    override suspend fun approveUser(id: String, userType: UserType): Result<Unit> = try {
        if (userType == UserType.BUYER) api.approvePharmacy(id) else api.approveSupplier(id)
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error("Failed to approve (${e.code()})")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    override suspend fun rejectUser(id: String, userType: UserType): Result<Unit> = try {
        if (userType == UserType.BUYER) api.rejectPharmacy(id) else api.rejectSupplier(id)
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error("Failed to reject (${e.code()})")
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
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e))
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load requests")
    }

    override suspend fun approveRequest(id: String, notes: String): Result<Unit> = try {
        api.approveRequest(id, notes.asBody())
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e))
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    override suspend fun declineRequest(id: String, reason: String): Result<Unit> = try {
        api.declineRequest(id, reason.asBody())
        Result.Success(Unit)
    } catch (e: HttpException) {
        Result.Error(parseHttpError(e))
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    private fun String.asBody() = toRequestBody("text/plain".toMediaTypeOrNull())

    private fun parseHttpError(e: HttpException): String {
        return try {
            val raw = e.response()?.errorBody()?.string()
                ?: return "Server error (${e.code()})"
            @Suppress("UNCHECKED_CAST")
            val json = Gson().fromJson(raw, Map::class.java) as? Map<String, Any>
                ?: return "Server error (${e.code()})"
            val errors = json["errors"]
            if (errors is Map<*, *>) {
                (errors.values.firstOrNull() as? List<*>)?.firstOrNull()?.toString()
                    ?: json["message"]?.toString()
                    ?: "Server error (${e.code()})"
            } else {
                json["message"]?.toString() ?: "Server error (${e.code()})"
            }
        } catch (ex: Exception) {
            "Server error (${e.code()})"
        }
    }
}
