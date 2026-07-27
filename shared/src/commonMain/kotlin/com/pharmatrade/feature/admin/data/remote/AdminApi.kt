package com.pharmatrade.feature.admin.data.remote

import com.pharmatrade.core.network.ApiClient
import com.pharmatrade.core.network.dto.ApiResponse
import com.pharmatrade.feature.admin.data.remote.dto.PendingUserDto
import com.pharmatrade.feature.admin.data.remote.dto.PendingUsersPageDto
import com.pharmatrade.feature.admin.data.remote.dto.RegistrationStatsDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post

class AdminApi(private val client: HttpClient = ApiClient.httpClient) {

    suspend fun getPendingPharmacies(status: String = "pending"): ApiResponse<List<PendingUserDto>> =
        client.get("admin/pharmacies") { parameter("status", status) }.body()

    suspend fun getPendingSuppliers(status: String = "pending"): ApiResponse<List<PendingUserDto>> =
        client.get("admin/suppliers") { parameter("status", status) }.body()

    suspend fun approvePharmacy(id: String) {
        client.post("admin/pharmacies/$id/approve")
    }

    suspend fun rejectPharmacy(id: String) {
        client.post("admin/pharmacies/$id/reject")
    }

    suspend fun approveSupplier(id: String) {
        client.post("admin/suppliers/$id/approve")
    }

    suspend fun rejectSupplier(id: String) {
        client.post("admin/suppliers/$id/reject")
    }

    // ── Unified registration-requests API ─────────────────────────────────────

    suspend fun getRegistrationStats(): ApiResponse<RegistrationStatsDto> =
        client.get("admin/registration-requests/stats").body()

    suspend fun getRegistrationRequests(
        status: String = "pending",
        entityType: String? = null
    ): ApiResponse<PendingUsersPageDto> =
        client.get("admin/registration-requests") {
            parameter("status", status)
            parameter("entity_type", entityType)
        }.body()

    suspend fun approveRequest(id: String, notes: String) {
        client.submitFormWithBinaryData(
            url = "admin/registration-requests/$id/approve",
            formData = formData { append("notes", notes) }
        )
    }

    suspend fun declineRequest(id: String, reason: String) {
        client.submitFormWithBinaryData(
            url = "admin/registration-requests/$id/decline",
            formData = formData { append("decline_reason", reason) }
        )
    }
}
