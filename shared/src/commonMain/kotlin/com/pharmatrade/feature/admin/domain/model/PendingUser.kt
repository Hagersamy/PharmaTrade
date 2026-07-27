package com.pharmatrade.feature.admin.domain.model

import com.pharmatrade.core.common.model.UserType

data class PendingUser(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val businessName: String,
    val licenceNumber: String?,
    val address: String?,
    val zoneId: String?,
    val userType: UserType,
    val status: String = "pending",
    // Licence images (URLs from server)
    val licenceFrontUrl: String? = null,
    val licenceBackUrl: String? = null,
    // Supplier-only
    val minOrderValue: String? = null,
    val minOrderQty: String? = null,
    val additionalZoneIds: List<String> = emptyList()
)
