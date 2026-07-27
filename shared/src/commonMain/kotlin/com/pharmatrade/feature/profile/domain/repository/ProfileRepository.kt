package com.pharmatrade.feature.profile.domain.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.profile.domain.model.BranchProfile
import com.pharmatrade.feature.profile.domain.model.ProfileInfo
import com.pharmatrade.feature.profile.domain.model.SupplierProfile
import com.pharmatrade.feature.profile.domain.model.ZoneUpdateRequest

interface ProfileRepository {
    suspend fun getProfile(): Result<ProfileInfo>
    suspend fun updateProfile(name: String?, email: String?, phone: String?, password: String): Result<ProfileInfo>
    suspend fun changePassword(email: String, currentPassword: String, newPassword: String, confirmPassword: String): Result<Unit>
    suspend fun updateSupplier(name: String, minOrderValue: Double, minOrderQty: Int): Result<SupplierProfile>
    suspend fun updateBranch(name: String, address: String, phone: String, licenceNumber: String): Result<BranchProfile>
    suspend fun deactivateAccount(phone: String): Result<Unit>
    suspend fun requestZoneUpdate(zoneIds: List<Int>, reason: String): Result<ZoneUpdateRequest>
}
