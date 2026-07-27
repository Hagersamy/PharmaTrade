package com.pharmatrade.feature.auth.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.core.network.RetrofitClient
import com.pharmatrade.feature.auth.data.remote.AuthApiService
import com.pharmatrade.feature.auth.data.remote.dto.LoginRequest
import com.pharmatrade.feature.auth.domain.repository.AuthRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.SocketTimeoutException

class AuthRepositoryImpl(private val context: Context) : AuthRepository {

    private val api = RetrofitClient.create<AuthApiService>()

    override suspend fun login(phone: String, password: String): Result<User> {
        return try {
            Log.d("AuthRepo", "LOGIN → phone=$phone, password=[${password.length} chars]")
            val response = api.login(LoginRequest(phone = phone, password = password))
            val userDto = response.data?.user ?: response.userRaw
            ?: run {
                Log.e(
                    "AuthRepo",
                    "LOGIN failed: backend returned no user | errorMessage=${response.errorMessage}"
                )
                return Result.Error(response.errorMessage ?: "Login failed")
            }
            val token = response.data?.resolvedToken ?: response.token ?: response.accessToken
            val user = userDto.toUser()
            SessionManager.login(user, token)
            Result.Success(user)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("AuthRepo", "LOGIN HttpException: code=${e.code()} body=$errorBody", e)
            Result.Error(parseHttpError(errorBody, e.code()))
        } catch (_: SocketTimeoutException) {
            Log.e("AuthRepo", "LOGIN failed: socket timeout")
            Result.Error("Connection timed out. Make sure the server is running and try again.")
        } catch (e: Exception) {
            Log.e("AuthRepo", "LOGIN failed: ${e::class.simpleName}: ${e.message}", e)
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
            val frontPart = licenceFrontUri?.uriToPart("licence_image")
            val backPart = licenceBackUri?.uriToPart("licence_image_back")
            Log.d(
                "AuthRepo",
                "REGISTER (${userType.name}) front=${frontPart != null} back=${backPart != null} zones=$zoneIds"
            )
            if (licenceFrontUri != null && frontPart == null)
                return Result.Error("Could not read licence front image. Please pick it again.")
            if (licenceBackUri != null && backPart == null)
                return Result.Error("Could not read licence back image. Please pick it again.")

            // @PartMap with zone_ids[0], zone_ids[1], … — indexed keys guarantee
            // the array reaches the server correctly (List<MultipartBody.Part> with
            // @Part is silently dropped by Retrofit due to Kotlin generic type erasure).
            val zoneMap = zoneIds.mapIndexed { index, id ->
                "zones[$index]" to id.asBody()
            }.toMap()
            Log.d("AuthRepo", "zone map size=${zoneMap.size} keys=${zoneMap.keys}")

            val response = if (userType == UserType.BUYER) {
                Log.d(
                    "AuthRepo", "registerPharmacy REQUEST:" +
                            "\n  name=$name" +
                            "\n  email=$email" +
                            "\n  phone=$phone" +
                            "\n  password=$password" +
                            "\n  password_confirmation=$password" +
                            "\n  business_name=$businessName" +
                            "\n  licence_number=$licenceNumber" +
                            "\n  address=$address" +
                            "\n  device_token=(empty)" +
                            "\n  licence_image=${frontPart?.headers}" +
                            "\n  licence_image_back=${backPart?.headers}" +
                            "\n  zones=$zoneIds"
                )
                api.registerPharmacy(
                    name = name.asBody(),
                    email = email.asBody(),
                    phone = phone.asBody(),
                    password = password.asBody(),
                    passwordConfirmation = password.asBody(),
                    businessName = businessName.asBody(),
                    licenceNumber = (licenceNumber ?: "").asBody(),
                    address = (address ?: "").asBody(),
                    deviceToken = "".asBody(),
                    licenceImage = frontPart,
                    licenceImageBack = backPart,
                    zoneMap = zoneMap
                )
            } else {
                Log.d(
                    "AuthRepo", "registerSupplier REQUEST:" +
                            "\n  name=$name" +
                            "\n  email=$email" +
                            "\n  phone=$phone" +
                            "\n  password=$password" +
                            "\n  password_confirmation=$password" +
                            "\n  business_name=$businessName" +
                            "\n  licence_number=$licenceNumber" +
                            "\n  address=$address" +
                            "\n  min_order_value=$minOrderValue" +
                            "\n  min_order_qty=$minOrderQty" +
                            "\n  device_token=(empty)" +
                            "\n  licence_image=${frontPart?.headers}" +
                            "\n  licence_image_back=${backPart?.headers}" +
                            "\n  zones=$zoneIds"
                )
                api.registerSupplier(
                    name = name.asBody(),
                    email = email.asBody(),
                    phone = phone.asBody(),
                    password = password.asBody(),
                    passwordConfirmation = password.asBody(),
                    businessName = businessName.asBody(),
                    licenceNumber = (licenceNumber ?: "").asBody(),
                    address = (address ?: "").asBody(),
                    minOrderValue = (minOrderValue ?: "0").asBody(),
                    minOrderQty = (minOrderQty ?: "0").asBody(),
                    deviceToken = "".asBody(),
                    licenceImage = frontPart,
                    licenceImageBack = backPart,
                    zoneMap = zoneMap
                )
            }

            Log.d("AuthRepo", "registerSupplier response=$response")

            val userDto = response.data?.user ?: response.userRaw
            if (userDto == null) {
                // Backend returned a pending-registration response (no user/token).
                // Treat 2xx with a message as success — user will see pending-approval screen.
                return Result.Success(
                    com.pharmatrade.core.common.model.User(
                        id = "", name = name, email = email, phone = phone,
                        userType = userType, businessName = businessName, status = "pending"
                    )
                )
            }
            val token = response.data?.resolvedToken ?: response.token ?: response.accessToken
            val user = userDto.toUser(defaultType = userType)
            SessionManager.login(user, token)
            Result.Success(user)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error. Check your connection.")
        }
    }

    override suspend fun logout(): Result<Unit> {
        SessionManager.logout()
        return Result.Success(Unit)
    }

    // --- Helpers ---

    private fun String.asBody() =
        toRequestBody("text/plain".toMediaTypeOrNull())

    private fun String.uriToPart(fieldName: String): MultipartBody.Part? {
        return try {
            val uri = Uri.parse(this)
            val stream = context.contentResolver.openInputStream(uri)
            if (stream == null) {
                Log.e(
                    "AuthRepo",
                    "uriToPart: openInputStream returned null for $fieldName | uri=$this"
                )
                return null
            }
            val bytes = stream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                Log.e("AuthRepo", "uriToPart: read 0 bytes for $fieldName | uri=$this")
                return null
            }
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            Log.d("AuthRepo", "uriToPart: $fieldName | mime=$mime | size=${bytes.size}B")
            val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            MultipartBody.Part.createFormData(fieldName, "upload.jpg", body)
        } catch (e: Exception) {
            Log.e(
                "AuthRepo",
                "uriToPart: failed for $fieldName | ${e::class.simpleName}: ${e.message}"
            )
            null
        }
    }

    private fun parseHttpError(e: HttpException): String =
        parseHttpError(e.response()?.errorBody()?.string(), e.code())

    private fun parseHttpError(raw: String?, code: Int): String {
        return try {
            raw ?: return "Server error ($code)"
            @Suppress("UNCHECKED_CAST")
            val json = Gson().fromJson(raw, Map::class.java) as? Map<String, Any>
                ?: return "Server error ($code)"
            val errors = json["errors"]
            if (errors is Map<*, *>) {
                (errors.values.firstOrNull() as? List<*>)?.firstOrNull()?.toString()
                    ?: json["message"]?.toString()
                    ?: "Server error ($code)"
            } else {
                json["message"]?.toString() ?: "Server error ($code)"
            }
        } catch (ex: Exception) {
            Log.e("AuthRepo", "parseHttpError: failed to parse backend error body | raw=$raw", ex)
            "Server error ($code)"
        }
    }
}
