package com.pharmatrade.feature.admin.domain.repository

import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.model.RegistrationStats

interface AdminRepository {
    suspend fun getPendingPharmacies(): Result<List<PendingUser>>
    suspend fun getPendingSuppliers(): Result<List<PendingUser>>
    suspend fun approveUser(id: String, userType: UserType): Result<Unit>
    suspend fun rejectUser(id: String, userType: UserType): Result<Unit>

    // Unified registration-requests endpoints
    suspend fun getRegistrationStats(): Result<RegistrationStats>
    suspend fun getRegistrationRequests(entityType: String?): Result<List<PendingUser>>
    suspend fun approveRequest(id: String, notes: String): Result<Unit>
    suspend fun declineRequest(id: String, reason: String): Result<Unit>
}
