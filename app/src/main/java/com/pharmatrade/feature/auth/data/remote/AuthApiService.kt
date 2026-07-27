package com.pharmatrade.feature.auth.data.remote

import com.pharmatrade.feature.auth.data.remote.dto.ApiResponse
import com.pharmatrade.feature.auth.data.remote.dto.AuthData
import com.pharmatrade.feature.auth.data.remote.dto.LoginRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface AuthApiService {

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<AuthData>

    @Multipart
    @POST("register/pharmacy")
    suspend fun registerPharmacy(
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part("password") password: RequestBody,
        @Part("password_confirmation") passwordConfirmation: RequestBody,
        @Part("business_name") businessName: RequestBody,
        @Part("licence_number") licenceNumber: RequestBody,
        @Part("address") address: RequestBody,
        @Part("device_token") deviceToken: RequestBody,
        @Part licenceImage: MultipartBody.Part?,
        @Part licenceImageBack: MultipartBody.Part?,
        @PartMap zoneMap: Map<String, @JvmSuppressWildcards RequestBody>
    ): ApiResponse<AuthData>

    @Multipart
    @POST("register/supplier")
    suspend fun registerSupplier(
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part("password") password: RequestBody,
        @Part("password_confirmation") passwordConfirmation: RequestBody,
        @Part("business_name") businessName: RequestBody,
        @Part("licence_number") licenceNumber: RequestBody,
        @Part("address") address: RequestBody,
        @Part("min_order_value") minOrderValue: RequestBody,
        @Part("min_order_qty") minOrderQty: RequestBody,
        @Part("device_token") deviceToken: RequestBody,
        @Part licenceImage: MultipartBody.Part?,
        @Part licenceImageBack: MultipartBody.Part?,
        @PartMap zoneMap: Map<String, @JvmSuppressWildcards RequestBody>
    ): ApiResponse<AuthData>
}
