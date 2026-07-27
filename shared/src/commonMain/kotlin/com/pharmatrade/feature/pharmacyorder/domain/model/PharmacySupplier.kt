package com.pharmatrade.feature.pharmacyorder.domain.model

import com.pharmatrade.feature.auth.domain.model.Zone

data class PharmacySupplier(
    val id: String,
    val name: String,
    val minOrderValue: Double,
    val minOrderQty: Int,
    val zones: List<Zone> = emptyList(),
    val inventoryCount: Int = 0,
    val lastInventoryUpdate: String = ""
)
