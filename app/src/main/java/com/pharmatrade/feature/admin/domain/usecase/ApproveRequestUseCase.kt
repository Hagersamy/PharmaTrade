package com.pharmatrade.feature.admin.domain.usecase

import com.pharmatrade.feature.admin.domain.repository.AdminRepository

class ApproveRequestUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke(id: String, notes: String = "") =
        repository.approveRequest(id, notes)
}
