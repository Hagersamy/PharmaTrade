package com.pharmatrade.feature.admin.data.repository

import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.data.remote.AdminApi
import com.pharmatrade.feature.admin.data.remote.dto.toRequest
import com.pharmatrade.feature.admin.domain.model.AdminForbiddenException
import com.pharmatrade.feature.admin.domain.model.DrugEnglishNameUpdate
import com.pharmatrade.feature.admin.domain.model.DrugField
import com.pharmatrade.feature.admin.domain.model.DrugFieldError
import com.pharmatrade.feature.admin.domain.model.DrugNotFoundException
import com.pharmatrade.feature.admin.domain.model.DrugValidationException
import com.pharmatrade.feature.admin.domain.model.MissingEnglishDrugsPage
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.model.RegistrationStats
import com.pharmatrade.feature.admin.domain.repository.AdminRepository
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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

    override suspend fun getDrugsMissingEnglish(page: Int): Result<MissingEnglishDrugsPage> = try {
        val response = api.getDrugsMissingEnglish(page)
        Result.Success(response.data?.toDomain() ?: MissingEnglishDrugsPage(emptyList(), page, page, 0))
    } catch (e: ResponseException) {
        drugHttpError(e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load drugs", e)
    }

    override suspend fun saveDrugEnglishName(id: String, update: DrugEnglishNameUpdate): Result<Unit> = try {
        api.updateDrug(id, update.toRequest())
        Result.Success(Unit)
    } catch (e: ResponseException) {
        drugHttpError(e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to save drug", e)
    }

    // 403 / 404 / 422 become typed exceptions the screen reacts to; anything else is a plain error.
    // (401 is handled app-wide by ApiClient.onUnauthorized, which logs the user out.)
    private suspend fun drugHttpError(e: ResponseException): Result.Error {
        val code = e.response.status.value
        val message = parseHttpError(e)
        return when (code) {
            403 -> Result.Error(message, AdminForbiddenException())
            404 -> Result.Error(message, DrugNotFoundException())
            422 -> {
                val fieldErrors = parseDrugFieldErrors(runCatching { e.response.bodyAsText() }.getOrNull())
                Result.Error(message, fieldErrors.takeIf { it.isNotEmpty() }?.let { DrugValidationException(it) } ?: e)
            }
            else -> Result.Error(message, e)
        }
    }

    // Laravel 422: { "message": "...", "errors": { "barcode": ["The barcode has already been taken."] } }
    private fun parseDrugFieldErrors(body: String?): Map<DrugField, DrugFieldError> {
        if (body.isNullOrBlank()) return emptyMap()
        val errors = runCatching { Json.parseToJsonElement(body).jsonObject["errors"]?.jsonObject }
            .getOrNull() ?: return emptyMap()
        return buildMap {
            errors.forEach { (key, value) ->
                val field = when (key) {
                    "name", "trade_name" -> DrugField.ENGLISH_NAME
                    "scientific_name" -> DrugField.SCIENTIFIC_NAME
                    "manufacturer" -> DrugField.MANUFACTURER
                    "dosage_form" -> DrugField.DOSAGE_FORM
                    "strength" -> DrugField.STRENGTH
                    "barcode" -> DrugField.BARCODE
                    else -> return@forEach
                }
                val text = (value as? JsonArray)?.firstOrNull()?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
                val error = when {
                    "taken" in text || "already" in text -> DrugFieldError.ALREADY_TAKEN
                    "may not be greater" in text || "must not be greater" in text -> DrugFieldError.TOO_LONG
                    "required" in text -> DrugFieldError.REQUIRED
                    "english" in text -> DrugFieldError.ENGLISH_ONLY
                    else -> DrugFieldError.INVALID
                }
                // name and trade_name both map to the one English-name input — keep the first.
                if (field !in this) put(field, error)
            }
        }
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
