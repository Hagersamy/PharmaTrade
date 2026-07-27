package com.pharmatrade.feature.auth.domain.usecase

import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.auth.domain.repository.AuthRepository

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        userType: UserType,
        businessName: String,
        licenceNumber: String? = null,
        zoneIds: List<String> = emptyList(),
        address: String? = null,
        licenceFrontUri: String? = null,
        licenceBackUri: String? = null,
        minOrderValue: String? = null,
        minOrderQty: String? = null
    ): Result<User> {
        if (name.isBlank()) return Result.Error("Name cannot be empty")
        if (email.isBlank() || !EMAIL_REGEX.matches(email))
            return Result.Error("Enter a valid email address")
        if (phone.isBlank()) return Result.Error("Phone number cannot be empty")
        if (password.length < 6) return Result.Error("Password must be at least 6 characters")
        if (password != confirmPassword) return Result.Error("Passwords do not match")
        if (businessName.isBlank()) return Result.Error("Business name cannot be empty")
        if (licenceNumber.isNullOrBlank()) return Result.Error("Licence number is required")
        if (zoneIds.isEmpty()) return Result.Error("Please select at least one zone")
        if (address.isNullOrBlank()) return Result.Error("Address is required")
        if (licenceFrontUri == null) return Result.Error("Please upload the front of your licence")
        if (licenceBackUri == null) return Result.Error("Please upload the back of your licence")
        if (userType == UserType.SELLER) {
            if (minOrderValue.isNullOrBlank()) return Result.Error("Minimum order value is required")
            if (minOrderQty.isNullOrBlank()) return Result.Error("Minimum order quantity is required")
        }
        return repository.register(
            name.trim(), email.trim(), phone.trim(), password, userType, businessName.trim(),
            licenceNumber.trim(), zoneIds.map { it.trim() }, address.trim(),
            licenceFrontUri, licenceBackUri,
            minOrderValue?.trim(), minOrderQty?.trim()
        )
    }
}
