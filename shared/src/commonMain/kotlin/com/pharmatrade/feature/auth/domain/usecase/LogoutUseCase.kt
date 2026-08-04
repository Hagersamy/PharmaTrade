package com.pharmatrade.feature.auth.domain.usecase

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.auth.domain.repository.AuthRepository

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.logout()
}
