package com.pharmatrade.feature.pharmacyorder.domain.model

data class SupplierCatalogItem(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val drugId: String,
    val drugName: String,
    val dosageForm: String = "",
    val strength: String = "",
    val quantityAvailable: Int,
    val unitPrice: Double,
    val publicPrice: Double,
    val pharmacistPrice: Double,
    val discountPct: Double,
    val effectivePrice: Double,
    // The backend row behind this item. [id] is supplierId_drugId (the UI/cart key), so two
    // inventory rows for the same supplier+drug share an id — these tell them apart in diagnostics.
    val inventoryId: String = "",
    val lastUpdated: String = ""
)
