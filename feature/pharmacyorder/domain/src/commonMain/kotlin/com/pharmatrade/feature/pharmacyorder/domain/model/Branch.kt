package com.pharmatrade.feature.pharmacyorder.domain.model

import com.pharmatrade.core.common.model.Zone

data class Branch(
    val id: String,
    val name: String,
    val address: String,
    val zones: List<Zone> = emptyList()
)
