package com.pharmatrade.core.common.error

import com.pharmatrade.core.common.i18n.Strings
import com.pharmatrade.core.common.result.Result

// Marks a Result.Error whose message is a business-rule message the backend wrote for the end
// user (e.g. a 4xx "minimum order value not reached" on allocate) — safe to show verbatim,
// unlike raw exception text or 5xx bodies, which still go through friendlyError.
class UserFacingException(message: String, cause: Throwable? = null) : Exception(message, cause)

// Shows the backend's own message when the data layer marked it user-facing; otherwise falls
// back to the curated friendlyError buckets.
fun Strings.userMessage(error: Result.Error): String =
    if (error.exception is UserFacingException && error.message.isNotBlank()) error.message
    else friendlyError(error.message)
