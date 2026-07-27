package com.pharmatrade.feature.drugs.domain.model

data class InventoryItem(
    val id: String,
    val drugName: String,
    val drugNameRaw: String,
    val isCatalogMatched: Boolean,
    val quantityAvailable: Int,
    val unitPrice: Double,
    val publicPrice: Double,
    val pharmacistPrice: Double,
    val discountPct: Double,
    val effectivePrice: Double,
    val lastUpdated: String,
    val catalogDrug: CatalogDrugInfo?
)

data class CatalogDrugInfo(
    val id: String,
    val name: String,
    val tradeName: String,
    val dosageForm: String,
    val strength: String
)

data class InventoryData(
    val items: List<InventoryItem>,
    val totalItems: Int,
    val currentPage: Int = 1,
    val lastPage: Int = 1,
    val lastUpload: UploadHistory?
) {
    val hasMorePages: Boolean get() = currentPage < lastPage
}
