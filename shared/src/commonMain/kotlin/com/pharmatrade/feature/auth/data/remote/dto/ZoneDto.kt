package com.pharmatrade.feature.auth.data.remote.dto

import com.pharmatrade.feature.auth.domain.model.Zone
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
