package com.pharmatrade.feature.auth.domain.usecase

import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.auth.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(phone: String, password: String): Result<User> {
        if (phone.isBlank()) return Result.Error("Phone number cannot be empty")
        if (password.length < 6) return Result.Error("Password must be at least 6 characters")
        return repository.login(phone.trim(), password)
    }
}
