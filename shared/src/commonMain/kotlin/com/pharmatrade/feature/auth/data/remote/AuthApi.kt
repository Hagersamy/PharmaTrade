package com.pharmatrade.feature.auth.data.remote

import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.FormFile
import com.pharmatrade.core.network.dto.ApiResponse
import com.pharmatrade.feature.auth.data.remote.dto.AuthData
import com.pharmatrade.feature.auth.data.remote.dto.LoginRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class AuthApi(private val client: HttpClient = ApiClient.httpClient) {

    suspend fun login(request: LoginRequest): ApiResponse<AuthData> =
        client.post("login") { setBody(request) }.body()

    suspend fun registerPharmacy(
        name: String,
        email: String,
        phone: String,
        password: String,
        passwordConfirmation: String,
        businessName: String,
        licenceNumber: String,
        address: String,
        deviceToken: String,
        zoneIds: List<String>,
        licenceImage: FormFile?,
        licenceImageBack: FormFile?
    ): ApiResponse<AuthData> = client.submitFormWithBinaryData(
        url = "register/pharmacy",
        formData = formData {
            append("name", name)
            append("email", email)
            append("phone", phone)
            append("password", password)
            append("password_confirmation", passwordConfirmation)
            append("business_name", businessName)
            append("licence_number", licenceNumber)
            append("address", address)
            append("device_token", deviceToken)
            zoneIds.forEachIndexed { index, id -> append("zones[$index]", id) }
            appendFile("licence_image", licenceImage)
            appendFile("licence_image_back", licenceImageBack)
        }
    ).body()

    suspend fun registerSupplier(
        name: String,
        email: String,
        phone: String,
        password: String,
        passwordConfirmation: String,
        businessName: String,
        licenceNumber: String,
        address: String,
        minOrderValue: String,
        minOrderQty: String,
        deviceToken: String,
        zoneIds: List<String>,
        licenceImage: FormFile?,
        licenceImageBack: FormFile?
    ): ApiResponse<AuthData> = client.submitFormWithBinaryData(
        url = "register/supplier",
        formData = formData {
            append("name", name)
            append("email", email)
            append("phone", phone)
            append("password", password)
            append("password_confirmation", passwordConfirmation)
            append("business_name", businessName)
            append("licence_number", licenceNumber)
            append("address", address)
            append("min_order_value", minOrderValue)
            append("min_order_qty", minOrderQty)
            append("device_token", deviceToken)
            zoneIds.forEachIndexed { index, id -> append("zones[$index]", id) }
            appendFile("licence_image", licenceImage)
            appendFile("licence_image_back", licenceImageBack)
        }
    ).body()
}

private fun FormBuilder.appendFile(fieldName: String, file: FormFile?) {
    if (file == null) return
    append(fieldName, file.bytes, Headers.build {
        append(HttpHeaders.ContentType, file.mimeType)
        append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
    })
}
