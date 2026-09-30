package com.pharmatrade.core.common.util

import kotlin.math.abs
import kotlin.math.round

// Multiplatform-safe replacement for String.format("%.Nf", value) — java.lang.String.format
// isn't available outside JVM targets, so screens can't use it once they move to commonMain.
fun formatDecimal(value: Double, decimals: Int): String {
    var factor = 1.0
    repeat(decimals) { factor *= 10 }
    val rounded = round(value * factor) / factor
    val isNegative = rounded < 0
    val absValue = abs(rounded)
    val intPart = absValue.toLong()
    if (decimals == 0) return (if (isNegative) "-" else "") + intPart.toString()
    val fracPart = round((absValue - intPart) * factor).toLong()
    val fracStr = fracPart.toString().padStart(decimals, '0')
    return (if (isNegative) "-" else "") + "$intPart.$fracStr"
}
