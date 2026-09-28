package com.pharmatrade.feature.auth.domain.usecase

import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.auth.domain.model.RegisterField
import com.pharmatrade.feature.auth.domain.model.RegisterFieldError
import com.pharmatrade.feature.auth.domain.model.RegisterValidationException
import com.pharmatrade.feature.auth.domain.repository.AuthRepository

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

// Egyptian mobile: 11 digits, 010 / 011 / 012 / 015 prefix (e.g. 01067487203).
private val PHONE_REGEX = Regex("^01[0125][0-9]{8}$")

// Users often type spaces or dashes ("010 6748 7203") — strip them before checking/sending.
fun normalizePhone(phone: String): String = phone.filterNot { it == ' ' || it == '-' }

class RegisterUseCase(private val repository: AuthRepository) {

    /** Checks every field at once (not just the first failure) so the form can flag them all. */
    fun validate(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        userType: UserType,
        businessName: String,
        licenceNumber: String?,
        zoneIds: List<String>,
        address: String?,
        licenceFrontUri: String?,
        licenceBackUri: String?,
        minOrderValue: String?,
        minOrderQty: String?
    ): Map<RegisterField, RegisterFieldError> = buildMap {
        if (name.isBlank()) put(RegisterField.NAME, RegisterFieldError.REQUIRED)
        if (businessName.isBlank()) put(RegisterField.BUSINESS_NAME, RegisterFieldError.REQUIRED)

        when {
            email.isBlank() -> put(RegisterField.EMAIL, RegisterFieldError.REQUIRED)
            !EMAIL_REGEX.matches(email.trim()) -> put(RegisterField.EMAIL, RegisterFieldError.INVALID_FORMAT)
        }

        val normalizedPhone = normalizePhone(phone)
        when {
            normalizedPhone.isEmpty() -> put(RegisterField.PHONE, RegisterFieldError.REQUIRED)
            !PHONE_REGEX.matches(normalizedPhone) -> put(RegisterField.PHONE, RegisterFieldError.INVALID_FORMAT)
        }

        when {
            password.isEmpty() -> put(RegisterField.PASSWORD, RegisterFieldError.REQUIRED)
            password.length < 6 -> put(RegisterField.PASSWORD, RegisterFieldError.TOO_SHORT)
        }
        when {
            confirmPassword.isEmpty() -> put(RegisterField.CONFIRM_PASSWORD, RegisterFieldError.REQUIRED)
            confirmPassword != password -> put(RegisterField.CONFIRM_PASSWORD, RegisterFieldError.MISMATCH)
        }

        if (licenceNumber.isNullOrBlank()) put(RegisterField.LICENCE_NUMBER, RegisterFieldError.REQUIRED)
        if (zoneIds.isEmpty()) put(RegisterField.ZONES, RegisterFieldError.REQUIRED)
        if (address.isNullOrBlank()) put(RegisterField.ADDRESS, RegisterFieldError.REQUIRED)
        if (licenceFrontUri == null) put(RegisterField.LICENCE_FRONT, RegisterFieldError.REQUIRED)
        if (licenceBackUri == null) put(RegisterField.LICENCE_BACK, RegisterFieldError.REQUIRED)

        if (userType == UserType.SELLER) {
            when {
                minOrderValue.isNullOrBlank() -> put(RegisterField.MIN_ORDER_VALUE, RegisterFieldError.REQUIRED)
                (minOrderValue.trim().toDoubleOrNull() ?: -1.0) < 0 ->
                    put(RegisterField.MIN_ORDER_VALUE, RegisterFieldError.INVALID)
            }
            when {
                minOrderQty.isNullOrBlank() -> put(RegisterField.MIN_ORDER_QTY, RegisterFieldError.REQUIRED)
                (minOrderQty.trim().toIntOrNull() ?: -1) < 0 ->
                    put(RegisterField.MIN_ORDER_QTY, RegisterFieldError.INVALID)
            }
        }
    }

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
        val errors = validate(
            name, email, phone, password, confirmPassword, userType, businessName, licenceNumber,
            zoneIds, address, licenceFrontUri, licenceBackUri, minOrderValue, minOrderQty
        )
        if (errors.isNotEmpty()) {
            return Result.Error("Registration input is invalid", RegisterValidationException(errors))
        }
        return repository.register(
            name.trim(), email.trim(), normalizePhone(phone), password, userType, businessName.trim(),
            licenceNumber!!.trim(), zoneIds.map { it.trim() }, address!!.trim(),
            licenceFrontUri, licenceBackUri,
            minOrderValue?.trim(), minOrderQty?.trim()
        )
    }
}
