package com.pharmatrade.core.common.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

private val MONTH_ABBREVIATIONS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private val DAY_NAMES = arrayOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)

// Backend timestamps arrive as UTC ISO-8601 with microseconds, e.g. "2026-06-22T22:32:15.000000Z".
// Displayed in the device's local time zone (parsed as UTC, formatted against
// TimeZone.currentSystemDefault(), matching the original java.text.SimpleDateFormat behavior).
fun formatBackendTimestamp(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val truncated = iso.substringBefore('.').removeSuffix("Z") + "Z"
        val instant = Instant.parse(truncated)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = MONTH_ABBREVIATIONS[local.monthNumber - 1]
        val hour12 = (local.hour % 12).let { if (it == 0) 12 else it }
        val amPm = if (local.hour < 12) "AM" else "PM"
        val minute = local.minute.toString().padStart(2, '0')
        "$month ${local.dayOfMonth}, ${local.year} · $hour12:$minute $amPm"
    }.getOrDefault(iso)
}

// Matches the original java.text.SimpleDateFormat("EEEE, MMM d") pattern, e.g. "Monday, Jul 27".
fun formatTodayLabel(): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val dayName = DAY_NAMES[today.dayOfWeek.ordinal]
    val month = MONTH_ABBREVIATIONS[today.monthNumber - 1]
    return "$dayName, $month ${today.dayOfMonth}"
}
