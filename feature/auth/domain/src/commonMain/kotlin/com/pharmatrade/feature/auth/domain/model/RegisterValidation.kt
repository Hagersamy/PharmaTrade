package com.pharmatrade.feature.auth.domain.model

// Every input on the register form that can be individually wrong — lets errors (local or from
// the backend's 422 `errors` object) be shown under the exact field instead of one generic banner.
enum class RegisterField {
    NAME, BUSINESS_NAME, EMAIL, PHONE, PASSWORD, CONFIRM_PASSWORD,
    LICENCE_NUMBER, ZONES, ADDRESS, LICENCE_FRONT, LICENCE_BACK,
    MIN_ORDER_VALUE, MIN_ORDER_QTY
}

enum class RegisterFieldError {
    REQUIRED,
    INVALID_FORMAT,
    TOO_SHORT,
    MISMATCH,
    ALREADY_TAKEN,
    INVALID
}

// Carried in Result.Error.exception so the presentation layer can highlight each bad field.
class RegisterValidationException(
    val fieldErrors: Map<RegisterField, RegisterFieldError>
) : Exception("Registration input is invalid: $fieldErrors")
