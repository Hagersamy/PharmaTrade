package com.pharmatrade.feature.auth.data.repository

import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.core.io.PlatformFileReader
import com.pharmatrade.core.network.FormFile
import com.pharmatrade.feature.auth.data.remote.AuthApi
import com.pharmatrade.feature.auth.data.remote.dto.LoginRequest
import com.pharmatrade.feature.auth.domain.repository.AuthRepository
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthRepositoryImpl(
    private val fileReader: PlatformFileReader,
    private val api: AuthApi = AuthApi()
) : AuthRepository {

    override suspend fun login(phone: String, password: String): Result<User> {
        return try {
            println("AuthRepo: LOGIN → phone=$phone, password=[${password.length} chars]")
            val response = api.login(LoginRequest(phone = phone, password = password))
            val userDto = response.data?.user ?: response.userRaw
            ?: run {
                println("AuthRepo: LOGIN failed: backend returned no user | errorMessage=${response.errorMessage}")
                return Result.Error(response.errorMessage ?: "Login failed")
            }
            val token = response.data?.resolvedToken ?: response.token ?: response.accessToken
            val user = userDto.toUser()
            SessionManager.login(user, token)
            Result.Success(user)
        } catch (e: ResponseException) {
            val errorBody = runCatching { e.response.bodyAsText() }.getOrNull()
            println("AuthRepo: LOGIN ResponseException: code=${e.response.status.value} body=$errorBody | $e")
            Result.Error(parseHttpError(errorBody, e.response.status.value))
        } catch (_: SocketTimeoutException) {
            println("AuthRepo: LOGIN failed: socket timeout")
            Result.Error("Connection timed out. Make sure the server is running and try again.")
        } catch (e: Exception) {
            println("AuthRepo: LOGIN failed: ${e::class.simpleName}: ${e.message}")
            Result.Error(e.message ?: "Network error. Check your connection.")
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        userType: UserType,
        businessName: String,
        licenceNumber: String?,
        zoneIds: List<String>,
        address: String?,
        licenceFrontUri: String?,
        licenceBackUri: String?,
        minOrderValue: String?,
        minOrderQty: String?
    ): Result<User> {
        return try {
            // Convert images first so we can surface an error before hitting the network
            val frontPart = licenceFrontUri?.uriToFormFile("licence_image")
            val backPart = licenceBackUri?.uriToFormFile("licence_image_back")
            println("AuthRepo: REGISTER (${userType.name}) front=${frontPart != null} back=${backPart != null} zones=$zoneIds")
            if (licenceFrontUri != null && frontPart == null)
                return Result.Error("Could not read licence front image. Please pick it again.")
            if (licenceBackUri != null && backPart == null)
                return Result.Error("Could not read licence back image. Please pick it again.")

            val response = if (userType == UserType.BUYER) {
                println(
                    "AuthRepo: registerPharmacy REQUEST:" +
                            "\n  name=$name" +
                            "\n  email=$email" +
                            "\n  phone=$phone" +
                            "\n  business_name=$businessName" +
                            "\n  licence_number=$licenceNumber" +
                            "\n  address=$address" +
                            "\n  zones=$zoneIds"
                )
                api.registerPharmacy(
                    name = name,
                    email = email,
                    phone = phone,
                    password = password,
                    passwordConfirmation = password,
                    businessName = businessName,
                    licenceNumber = licenceNumber ?: "",
                    address = address ?: "",
                    deviceToken = "",
                    zoneIds = zoneIds,
                    licenceImage = frontPart,
                    licenceImageBack = backPart
                )
            } else {
                println(
                    "AuthRepo: registerSupplier REQUEST:" +
                            "\n  name=$name" +
                            "\n  email=$email" +
                            "\n  phone=$phone" +
                            "\n  business_name=$businessName" +
                            "\n  licence_number=$licenceNumber" +
                            "\n  address=$address" +
                            "\n  min_order_value=$minOrderValue" +
                            "\n  min_order_qty=$minOrderQty" +
                            "\n  zones=$zoneIds"
                )
                api.registerSupplier(
                    name = name,
                    email = email,
                    phone = phone,
                    password = password,
                    passwordConfirmation = password,
                    businessName = businessName,
                    licenceNumber = licenceNumber ?: "",
                    address = address ?: "",
                    minOrderValue = minOrderValue ?: "0",
                    minOrderQty = minOrderQty ?: "0",
                    deviceToken = "",
                    zoneIds = zoneIds,
                    licenceImage = frontPart,
                    licenceImageBack = backPart
                )
            }

            println("AuthRepo: register response=$response")

            val userDto = response.data?.user ?: response.userRaw
            if (userDto == null) {
                // Backend returned a pending-registration response (no user/token).
                // Treat 2xx with a message as success — user will see pending-approval screen.
                return Result.Success(
                    User(
                        id = "", name = name, email = email, phone = phone,
                        userType = userType, businessName = businessName, status = "pending"
                    )
                )
            }
            val token = response.data?.resolvedToken ?: response.token ?: response.accessToken
            val user = userDto.toUser(defaultType = userType)
            SessionManager.login(user, token)
            Result.Success(user)
        } catch (e: ResponseException) {
            val errorBody = runCatching { e.response.bodyAsText() }.getOrNull()
            Result.Error(parseHttpError(errorBody, e.response.status.value))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error. Check your connection.")
        }
    }

    override suspend fun logout(): Result<Unit> {
        SessionManager.logout()
        return Result.Success(Unit)
    }

    // --- Helpers ---

    private fun String.uriToFormFile(fieldName: String): FormFile? {
        val info = fileReader.read(this)
        if (info == null) {
            println("AuthRepo: uriToFormFile: failed to read $fieldName | uri=$this")
            return null
        }
        val mime = info.mimeType ?: "image/jpeg"
        println("AuthRepo: uriToFormFile: $fieldName | mime=$mime | size=${info.bytes.size}B")
        return FormFile(bytes = info.bytes, fileName = "upload.jpg", mimeType = mime)
    }

    private fun parseHttpError(raw: String?, code: Int): String {
        return try {
            raw ?: return "Server error ($code)"
            val json = Json.parseToJsonElement(raw).jsonObject
            val errors = json["errors"]?.jsonObject
            if (errors != null && errors.isNotEmpty()) {
                errors.values.firstOrNull()?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                    ?: json["message"]?.jsonPrimitive?.contentOrNull
                    ?: "Server error ($code)"
            } else {
                json["message"]?.jsonPrimitive?.contentOrNull ?: "Server error ($code)"
            }
        } catch (ex: Exception) {
            println("AuthRepo: parseHttpError: failed to parse backend error body | raw=$raw | $ex")
            "Server error ($code)"
        }
    }
}
