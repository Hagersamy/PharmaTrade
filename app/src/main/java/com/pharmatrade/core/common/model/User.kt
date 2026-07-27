package com.pharmatrade.core.common.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val userType: UserType,
    val status: String? = null, // "pending", "active", "rejected"; null treated as active
    val businessName: String = "",
    val sellerId: String? = null,
    val licenceNumber: String? = null,
    val zoneId: String? = null,
    val address: String? = null,
    val licenceFrontUri: String? = null,
    val licenceBackUri: String? = null,
    val additionalZoneIds: List<String> = emptyList(),
    val minOrderValue: String? = null,
    val minOrderQty: String? = null
) {
    val isPending: Boolean get() = status?.lowercase() == "pending"
}

enum class UserType(val displayName: String) {
    SELLER("Drug Seller Agent"),
    BUYER("Pharmacy"),
    ADMIN("Admin")
}
