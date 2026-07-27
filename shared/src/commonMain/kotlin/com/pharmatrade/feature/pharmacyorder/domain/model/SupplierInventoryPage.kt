package com.pharmatrade.feature.pharmacyorder.domain.model

data class SupplierInventoryPage(
    val supplier: PharmacySupplier?,
    val items: List<SupplierInventoryItem>
)
