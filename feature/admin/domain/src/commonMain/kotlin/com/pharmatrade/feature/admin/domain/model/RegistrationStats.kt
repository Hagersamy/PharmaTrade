package com.pharmatrade.feature.admin.domain.model

data class RegistrationStats(
    val pending: Int = 0,
    val approved: Int = 0,
    val declined: Int = 0,
    val total: Int = 0,
    val pendingPharmacies: Int = 0,
    val pendingSuppliers: Int = 0
)
