package com.pharmatrade.feature.auth.domain.repository

import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result

interface AuthRepository {
    suspend fun login(phone: String, password: String): Result<User>
    suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        userType: UserType,
        businessName: String,
        licenceNumber: String? = null,
        zoneIds: List<String> = emptyList(),
        address: String? = null,
        licenceFrontUri: String? = null,
        licenceBackUri: String? = null,
        minOrderValue: String? = null,
        minOrderQty: String? = null
    ): Result<User>
    suspend fun logout(): Result<Unit>
}
