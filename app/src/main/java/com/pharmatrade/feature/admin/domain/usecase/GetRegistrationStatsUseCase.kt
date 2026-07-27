package com.pharmatrade.feature.admin.domain.usecase

import com.pharmatrade.feature.admin.domain.repository.AdminRepository

class GetRegistrationStatsUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke() = repository.getRegistrationStats()
}
