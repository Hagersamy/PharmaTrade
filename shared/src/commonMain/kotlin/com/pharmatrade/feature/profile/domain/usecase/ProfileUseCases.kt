package com.pharmatrade.feature.profile.domain.usecase

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.profile.domain.model.BranchProfile
import com.pharmatrade.feature.profile.domain.model.ProfileInfo
import com.pharmatrade.feature.profile.domain.model.SupplierProfile
import com.pharmatrade.feature.profile.domain.model.ZoneUpdateRequest
import com.pharmatrade.feature.profile.domain.repository.ProfileRepository

class GetProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(): Result<ProfileInfo> = repository.getProfile()
}

class UpdateProfileUseCase(private val repository: ProfileRepository) {
    // name/email/phone are null when unchanged — only edited fields are sent to the backend,
    // alongside the password that's always required to confirm the change.
    suspend operator fun invoke(name: String?, email: String?, phone: String?, password: String): Result<ProfileInfo> {
        if (password.isBlank()) return Result.Error("Password is required to confirm this change")
        if (name != null && name.isBlank()) return Result.Error("Name cannot be empty")
        if (email != null && email.isBlank()) return Result.Error("Email cannot be empty")
        if (phone != null && phone.isBlank()) return Result.Error("Phone number cannot be empty")
        if (name == null && email == null && phone == null) return Result.Error("Change at least one field")
        return repository.updateProfile(name?.trim(), email?.trim(), phone?.trim(), password)
    }
}

class ChangePasswordUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(
        email: String,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> {
        if (email.isBlank()) return Result.Error("Email is required")
        if (currentPassword.isBlank()) return Result.Error("Current password is required")
        if (newPassword.length < 6) return Result.Error("New password must be at least 6 characters")
        if (newPassword != confirmPassword) return Result.Error("Passwords do not match")
        return repository.changePassword(email.trim(), currentPassword, newPassword, confirmPassword)
    }
}

class UpdateSupplierProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(name: String, minOrderValue: Double, minOrderQty: Int): Result<SupplierProfile> {
        if (name.isBlank()) return Result.Error("Name cannot be empty")
        if (minOrderValue < 0) return Result.Error("Minimum order value cannot be negative")
        if (minOrderQty < 0) return Result.Error("Minimum order quantity cannot be negative")
        return repository.updateSupplier(name.trim(), minOrderValue, minOrderQty)
    }
}

class UpdateBranchProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(
        name: String,
        address: String,
        phone: String,
        licenceNumber: String
    ): Result<BranchProfile> {
        if (name.isBlank()) return Result.Error("Branch name cannot be empty")
        if (address.isBlank()) return Result.Error("Address cannot be empty")
        if (phone.isBlank()) return Result.Error("Phone cannot be empty")
        return repository.updateBranch(name.trim(), address.trim(), phone.trim(), licenceNumber.trim())
    }
}

class RequestZoneUpdateUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(zoneIds: List<Int>, reason: String): Result<ZoneUpdateRequest> {
        if (zoneIds.isEmpty()) return Result.Error("Select at least one zone")
        if (reason.isBlank()) return Result.Error("A reason is required")
        return repository.requestZoneUpdate(zoneIds, reason.trim())
    }
}
