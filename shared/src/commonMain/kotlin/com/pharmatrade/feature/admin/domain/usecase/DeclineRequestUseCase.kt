package com.pharmatrade.feature.admin.domain.usecase

import com.pharmatrade.feature.admin.domain.repository.AdminRepository

class DeclineRequestUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke(id: String, reason: String) =
        repository.declineRequest(id, reason)
}
