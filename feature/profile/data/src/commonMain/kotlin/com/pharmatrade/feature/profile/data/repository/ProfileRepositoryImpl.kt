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
import com.pharmatrade.core.io.PlatformFileReader
import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.FormFile
import com.pharmatrade.core.network.dto.ApiResponse
import com.pharmatrade.feature.profile.data.remote.dto.ProfileResponseDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ProfileRepositoryImpl(
    private val fileReader: PlatformFileReader,
    private val api: ProfileApi = ProfileApi()
) : ProfileRepository {

    override suspend fun getProfile(): Result<ProfileInfo> = try {
        val raw = api.getProfile()
        val response = ApiClient.json.decodeFromJsonElement<ApiResponse<ProfileResponseDto>>(raw)
        val parsed = response.data?.toDomain()
            ?: return Result.Error(response.errorMessage ?: "Profile not found")
        // Live reply (2026-10-27): GET /profile's "supplier" has no logo field at all, though the
        // backend stores one (the catalog returns it). Until the backend adds supplier.logo_url,
        // fall back to any logo URL in the reply, then to the fixed Cloudinary path every
        // supplier logo is uploaded to — a 404 there just leaves the generic avatar showing.
        val profile = parsed.supplier
            ?.takeIf { it.logoUrl == null }
            ?.let { supplier ->
                // A URL saved from this session's upload is versioned, so it beats the fixed path
                // (whose CDN copy can lag behind a re-upload).
                val logoUrl = (raw as? JsonObject)?.get("data")?.findLogoUrl()
                    ?: SessionManager.user?.logoUrl
                    ?: supplier.id.takeIf { it != "0" }?.let { "$SUPPLIER_LOGO_BASE_URL/supplier_$it.jpg" }
                parsed.copy(supplier = supplier.copy(logoUrl = logoUrl))
            }
            ?: parsed
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

    override suspend fun uploadLogo(imageUri: String): Result<String?> = try {
        val info = fileReader.read(imageUri)
            ?: return Result.Error("Could not read the image. Please pick it again.")
        val file = FormFile(bytes = info.bytes, fileName = "logo.jpg", mimeType = info.mimeType ?: "image/jpeg")
        val logoUrl = api.uploadLogo(file).findLogoUrl()
        if (logoUrl != null) {
            SessionManager.user?.let { SessionManager.login(it.copy(logoUrl = logoUrl), SessionManager.authToken) }
        }
        Result.Success(logoUrl)
    } catch (e: ResponseException) {
        Result.Error(parseHttpError(e), e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update logo", e)
    }

    // Live reply (2026-10-26): {"message": ..., "logo_url": "https://res.cloudinary.com/..."}.
    // Searched by key anywhere in the body so a later move under "data" keeps working.
    private fun JsonElement.findLogoUrl(): String? = when (this) {
        is JsonObject -> listOf("logo_url", "logo")
            .firstNotNullOfOrNull { key -> (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.startsWith("http") } }
            ?: values.firstNotNullOfOrNull { it.findLogoUrl() }
        is JsonArray -> firstNotNullOfOrNull { it.findLogoUrl() }
        else -> null
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
                address = profile.branch?.address ?: current.address,
                logoUrl = profile.supplier?.logoUrl ?: current.logoUrl
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

    private companion object {
        // Where POST supplier/profile/logo stores logos (seen in its reply and the catalog's logo_url).
        const val SUPPLIER_LOGO_BASE_URL = "https://res.cloudinary.com/ktzgfzpc/image/upload/suppliers/logos"
    }
}
