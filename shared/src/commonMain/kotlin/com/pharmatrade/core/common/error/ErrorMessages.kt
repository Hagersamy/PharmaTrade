package com.pharmatrade.core.common.error

import com.pharmatrade.core.common.i18n.Strings

// Classifies a raw error (backend message body or exception text) into one of a small set of
// curated, localized, user-safe messages. Never returns the raw text itself — backend/DB/
// exception details (status codes, column names, stack traces, etc.) must never reach a
// toast/snackbar/error screen.
fun Strings.friendlyError(raw: String?): String {
    val text = raw?.lowercase() ?: return errorGeneric
    if (text.isBlank()) return errorGeneric

    // Known local validation messages (authored in domain usecases, never backend/DB-sourced)
    // — map 1:1 to their own localized string so specific, actionable feedback isn't lost to
    // the generic fallback below.
    when (text) {
        "name cannot be empty" -> return errorNameRequired
        "enter a valid email address" -> return errorInvalidEmail
        "phone number cannot be empty" -> return errorPhoneRequired
        "password must be at least 6 characters" -> return errorPasswordTooShort
        "passwords do not match" -> return regPasswordsDoNotMatch
        "business name cannot be empty" -> return errorBusinessNameRequired
        "licence number is required" -> return errorLicenceNumberRequired
        "please select at least one zone" -> return errorZoneRequired
        "address is required" -> return errorAddressRequired
        "please upload the front of your licence" -> return errorLicenceFrontRequired
        "please upload the back of your licence" -> return errorLicenceBackRequired
        "minimum order value is required" -> return errorInvalidMinOrderValue
        "minimum order quantity is required" -> return errorInvalidMinOrderQty
        "no file selected" -> return errorNoFileSelected
        "drug name is required" -> return errorDrugNameRequired
        "trade name is required" -> return errorTradeNameRequired
        "select a drug" -> return errorSelectDrug
        "quantity must be greater than 0", "quantity cannot be negative" -> return errorInvalidQuantity
        "price must be greater than 0", "price cannot be negative" -> return errorInvalidPrice
        "discount must be between 0 and 99%", "discount must be between 0 and 100" -> return errorInvalidDiscount
        else -> Unit
    }

    val credentialHints = listOf(
        "invalid credentials", "incorrect password", "invalid password", "wrong password",
        "unauthorized", "unauthenticated", "authentication failed", "invalid phone or password",
        "login failed", "invalid login", "credentials do not match", "credentials"
    )
    if (credentialHints.any { text.contains(it) }) return errorInvalidCredentials

    if (text.contains("email") && (text.contains("taken") || text.contains("already") || text.contains("exists"))) {
        return errorEmailInUse
    }

    // Backend phone validation on register (e.g. "The phone has already been taken.",
    // "The phone format is invalid.") — RegisterUseCase only checks for blank locally, so any
    // duplicate/malformed phone is only ever caught server-side and needs its own bucket here,
    // otherwise it falls through to the generic error below with no actionable detail.
    if (text.contains("phone")) return errorInvalidPhone

    val networkHints = listOf(
        "timeout", "timed out", "unable to resolve host", "failed to connect",
        "no address associated", "network is unreachable", "connection refused",
        "connection reset", "connect timed out", "unresolved address"
    )
    if (networkHints.any { text.contains(it) }) return errorNetwork

    return errorGeneric
}
