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
    val additionalZoneIds: List<String> = emptyList(),
    // Zone-update requests (entity_type=zone_update) — an existing user asking to change the
    // zones they serve, not a new registration. Approved/declined through the same endpoints.
    val isZoneUpdate: Boolean = false,
    val zoneUpdateReason: String? = null,
    val requestedZoneIds: List<String> = emptyList(),
    val currentZoneIds: List<String> = emptyList(),
    // Zone id -> name, when the server sends zone objects rather than bare ids.
    val serverZoneNames: Map<String, String> = emptyMap(),
    val submittedAt: String? = null
)
