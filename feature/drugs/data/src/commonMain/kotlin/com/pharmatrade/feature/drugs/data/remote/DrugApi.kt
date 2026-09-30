package com.pharmatrade.feature.drugs.data.remote

import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.FormFile
import com.pharmatrade.core.network.dto.ApiResponse
import com.pharmatrade.feature.drugs.data.remote.dto.CreateDrugRequest
import com.pharmatrade.feature.drugs.data.remote.dto.DrugDto
import com.pharmatrade.feature.drugs.data.remote.dto.DrugPageDto
import com.pharmatrade.feature.drugs.data.remote.dto.InventoryItemDto
import com.pharmatrade.feature.drugs.data.remote.dto.InventoryItemRequest
import com.pharmatrade.feature.drugs.data.remote.dto.InventoryResponseDto
import com.pharmatrade.feature.drugs.data.remote.dto.UploadHistoryDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class DrugApi(private val client: HttpClient = ApiClient.httpClient) {

    suspend fun getDrugs(
        search: String? = null,
        dosageForm: String? = null,
        perPage: Int? = null
    ): ApiResponse<DrugPageDto> = client.get("drugs") {
        parameter("search", search)
        parameter("dosage_form", dosageForm)
        parameter("per_page", perPage)
    }.body()

    suspend fun getDrugById(id: String): ApiResponse<DrugDto> =
        client.get("drugs/$id").body()

    suspend fun getDosageForms(): ApiResponse<List<String>> =
        client.get("drugs/dosage-forms").body()

    suspend fun createDrug(request: CreateDrugRequest): ApiResponse<DrugDto> =
        client.post("admin/drugs") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun createSupplierDrug(request: CreateDrugRequest): ApiResponse<DrugDto> =
        client.post("supplier/drugs") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun uploadInventory(file: FormFile): ApiResponse<UploadHistoryDto> =
        client.submitFormWithBinaryData(
            url = "supplier/inventory/upload",
            formData = formData {
                append(
                    "file", file.bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, file.mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
                    }
                )
            }
        ).body()

    suspend fun getUploadHistory(): ApiResponse<List<UploadHistoryDto>> =
        client.get("supplier/inventory/upload-history").body()

    // per_page is deliberately not exposed here — sending it was found to change what the
    // backend scopes results to (see DrugRepositoryImpl.getInventory). Page through with the
    // backend's own default page size only.
    suspend fun getInventory(page: Int? = null): ApiResponse<InventoryResponseDto> =
        client.get("supplier/inventory") { parameter("page", page) }.body()

    suspend fun createInventoryItem(request: InventoryItemRequest): ApiResponse<InventoryItemDto> =
        client.post("supplier/inventory") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun updateInventoryItem(id: String, request: InventoryItemRequest): ApiResponse<InventoryItemDto> =
        client.put("supplier/inventory/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
