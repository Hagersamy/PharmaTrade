package com.pharmatrade.feature.profile.domain.model

import com.pharmatrade.feature.auth.domain.model.Zone

data class ProfileInfo(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val status: String?,
    val supplier: SupplierProfile? = null,
    val branch: BranchProfile? = null
)

data class SupplierProfile(
    val id: String,
    val name: String,
    val licenceNumber: String?,
    val minOrderValue: String,
    val minOrderQty: Int,
    val zones: List<Zone> = emptyList()
)

data class BranchProfile(
    val id: String,
    val name: String,
    val address: String,
    val phone: String,
    val licenceNumber: String?
)

data class ZoneUpdateRequest(
    val requestId: String,
    val status: String,
    val zoneIds: List<Int>,
    val submittedAt: String
)
