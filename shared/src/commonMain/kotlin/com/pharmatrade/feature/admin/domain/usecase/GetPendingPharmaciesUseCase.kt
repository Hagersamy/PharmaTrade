package com.pharmatrade.feature.admin.domain.usecase

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.repository.AdminRepository

class GetPendingPharmaciesUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke(): Result<List<PendingUser>> = repository.getPendingPharmacies()
}
