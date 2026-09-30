package com.pharmatrade.core.network.dto

import com.pharmatrade.core.common.model.Zone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZoneDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("governorate") val governorate: String? = null
) {
    fun toZone() = Zone(id = id, name = name, governorate = governorate)
}
