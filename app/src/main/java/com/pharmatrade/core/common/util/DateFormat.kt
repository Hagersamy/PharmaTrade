package com.pharmatrade.core.common.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Backend timestamps arrive as UTC ISO-8601 with microseconds, e.g. "2026-06-22T22:32:15.000000Z".
fun formatBackendTimestamp(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(iso.substringBefore('.').removeSuffix("Z"))
        val formatter = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        date?.let { formatter.format(it) } ?: iso
    }.getOrDefault(iso)
}
