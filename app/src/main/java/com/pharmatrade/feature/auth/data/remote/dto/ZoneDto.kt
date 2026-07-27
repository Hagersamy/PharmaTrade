package com.pharmatrade.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.pharmatrade.feature.auth.domain.model.Zone

data class ZoneDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("governorate") val governorate: String? = null
) {
    fun toZone() = Zone(id = id, name = name, governorate = governorate)
}
