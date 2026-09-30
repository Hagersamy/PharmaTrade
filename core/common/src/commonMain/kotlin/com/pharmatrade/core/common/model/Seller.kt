package com.pharmatrade.core.common.model

import kotlinx.serialization.Serializable

@Serializable
data class Seller(
    val id: String,
    val userId: String,
    val businessName: String,
    val ownerName: String,
    val phone: String,
    val location: String,
    val rating: Float,
    val minimumOrderAmount: Double,
    val totalSales: Int,
    val isVerified: Boolean,
    val licenseNumber: String
)
