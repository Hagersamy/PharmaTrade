package com.pharmatrade.core.network.dto

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

// Some backend fields arrive as either a JSON number or a JSON string depending on the
// endpoint (ids, row counters, order minimums) — read either shape uniformly.
fun JsonElement?.rawStringOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

fun JsonElement?.rawIntOrZero(): Int {
    val primitive = this as? JsonPrimitive ?: return 0
    return primitive.intOrNull ?: primitive.content.toDoubleOrNull()?.toInt() ?: 0
}

fun JsonElement?.rawDoubleOrZero(): Double {
    val primitive = this as? JsonPrimitive ?: return 0.0
    return primitive.doubleOrNull ?: primitive.content.toDoubleOrNull() ?: 0.0
}
