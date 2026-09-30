package com.pharmatrade.core.common.model

data class Zone(val id: Int, val name: String, val governorate: String? = null) {
    val displayName: String
        get() = if (governorate != null) "$name - $governorate" else name
}
