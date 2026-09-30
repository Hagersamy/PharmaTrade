package com.pharmatrade.feature.profile.data.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.feature.profile.data.remote.ProfileApi
import com.pharmatrade.feature.profile.data.remote.dto.ChangePasswordRequest
import com.pharmatrade.feature.profile.data.remote.dto.RequestZoneUpdateRequest
import com.pharmatrade.feature.profile.data.remote.dto.UpdateBranchRequest
import com.pharmatrade.feature.profile.data.remote.dto.UpdateSupplierRequest
import com.pharmatrade.feature.profile.domain.model.BranchProfile
import com.pharmatrade.feature.profile.domain.model.ProfileInfo
import com.pharmatrade.feature.profile.domain.model.SupplierProfile
import com.pharmatrade.feature.profile.domain.model.ZoneUpdateRequest
import com.pharmatrade.feature.profile.domain.repository.ProfileRepository
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ProfileRepositoryImpl(
    private val api: ProfileApi = ProfileApi()
) : ProfileRepository {

    override suspend fun getProfile(): Result<ProfileInfo> = try {
        val response = api.getProfile()
        val profile = response.data?.toDomain()
            ?: return Result.Error(response.errorMessage ?: "Profile not found")
        syncSession(profile)
        Result.Success(profile)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load profile", e)
    }

    override suspend fun updateProfile(
        name: String?,
        email: String?,
        phone: String?,
        password: String
    ): Result<ProfileInfo> = try {
        val body = mutableMapOf("password" to password)
        if (name != null) body["name"] = name
        if (email != null) body["email"] = email
        if (phone != null) body["phone"] = phone

        val response = api.updateProfile(body)
        val profile = response.data?.toDomain()
            ?: return Result.Error(response.errorMessage ?: "Failed to update profile")
        syncSession(profile)
        Result.Success(profile)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update profile", e)
    }

    override suspend fun changePassword(
        email: String,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> = try {
        val response = api.changePassword(
            ChangePasswordRequest(
                email = email,
                currentPassword = currentPassword,
                password = newPassword,
                passwordConfirmation = confirmPassword
            )
        )
        if (response.isSuccessful) Result.Success(Unit)
        else Result.Error(response.errorMessage ?: "Failed to change password")
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to change password", e)
    }

    override suspend fun updateSupplier(
        name: String,
        minOrderValue: Double,
        minOrderQty: Int
    ): Result<SupplierProfile> = try {
        val response = api.updateSupplier(UpdateSupplierRequest(name, minOrderValue, minOrderQty))
        val supplier = response.data?.toDomain()
            ?: return Result.Error(response.errorMessage ?: "Failed to update supplier info")
        SessionManager.user?.let { current ->
            SessionManager.login(
                current.copy(
                    businessName = supplier.name,
                    minOrderValue = supplier.minOrderValue,
                    minOrderQty = supplier.minOrderQty.toString()
                ),
                SessionManager.authToken
            )
        }
        Result.Success(supplier)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update supplier info", e)
    }

    override suspend fun updateBranch(
        name: String,
        address: String,
        phone: String,
        licenceNumber: String
    ): Result<BranchProfile> = try {
        val response = api.updateBranch(UpdateBranchRequest(name, address, phone, licenceNumber))
        val branch = response.data?.toDomain()
            ?: return Result.Error(response.errorMessage ?: "Failed to update branch")
        SessionManager.user?.let { current ->
            SessionManager.login(
                current.copy(businessName = branch.name, address = branch.address),
                SessionManager.authToken
            )
        }
        Result.Success(branch)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update branch", e)
    }

    override suspend fun requestZoneUpdate(zoneIds: List<Int>, reason: String): Result<ZoneUpdateRequest> = try {
        val response = api.requestZoneUpdate(RequestZoneUpdateRequest(zoneIds, reason))
        val request = response.data?.toDomain()
            ?: return Result.Error(response.errorMessage ?: "Failed to submit zone update request")
        Result.Success(request)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to submit zone update request", e)
    }

    private fun syncSession(profile: ProfileInfo) {
        val current = SessionManager.user ?: return
        SessionManager.login(
            current.copy(
                name = profile.name,
                email = profile.email,
                phone = profile.phone,
                businessName = profile.supplier?.name ?: profile.branch?.name ?: current.businessName,
                licenceNumber = profile.supplier?.licenceNumber ?: profile.branch?.licenceNumber
                    ?: current.licenceNumber,
                minOrderValue = profile.supplier?.minOrderValue ?: current.minOrderValue,
                minOrderQty = profile.supplier?.minOrderQty?.toString() ?: current.minOrderQty,
                address = profile.branch?.address ?: current.address
            ),
            SessionManager.authToken
        )
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
