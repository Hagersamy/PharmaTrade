package com.pharmatrade.feature.admin.domain.usecase

import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.domain.repository.AdminRepository

class ApproveUserUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke(id: String, userType: UserType): Result<Unit> =
        repository.approveUser(id, userType)
}
