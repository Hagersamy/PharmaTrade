package com.pharmatrade.feature.admin.domain.usecase

import com.pharmatrade.feature.admin.domain.repository.AdminRepository

class GetRegistrationRequestsUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke(entityType: String? = null) =
        repository.getRegistrationRequests(entityType)
}
