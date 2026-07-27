package com.pharmatrade.feature.drugs.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.model.DrugCategory
import com.pharmatrade.feature.drugs.domain.model.CatalogDrugInfo
import com.pharmatrade.feature.drugs.domain.model.InventoryData
import com.pharmatrade.feature.drugs.domain.model.InventoryItem
import com.pharmatrade.feature.drugs.domain.model.UploadHistory

data class DrugDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("trade_name") val tradeName: String? = null,
    @SerializedName("scientific_name") val scientificName: String? = null,
    @SerializedName("manufacturer") val manufacturer: String? = null,
    @SerializedName("dosage_form") val dosageForm: String? = null,
    @SerializedName("strength") val strength: String? = null,
    @SerializedName("barcode") val barcode: String? = null
) {
    fun toDomain(): Drug = Drug(
        id = id?.toString() ?: "",
        name = tradeName?.takeIf { it.isNotBlank() } ?: name ?: "",
        genericName = scientificName ?: "",
        category = DrugCategory.OTHER,
        manufacturer = manufacturer ?: "",
        description = name ?: "",
        dosageForm = dosageForm ?: "",
        strength = strength ?: ""
    )
}

// GET /drugs returns a Laravel paginator wrapping the drug array, not a flat list.
data class DrugPageDto(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("data") val data: List<DrugDto>? = null,
    @SerializedName("last_page") val lastPage: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    @SerializedName("total") val total: Int? = null
)

data class UploadHistoryDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("total_rows") val totalRows: Any? = null,
    @SerializedName("processed_rows") val processedRows: Any? = null,
    @SerializedName("failed_rows") val failedRows: Any? = null
) {
    fun toDomain() = UploadHistory(
        id = id?.toString() ?: "",
        fileName = fileName ?: "",
        status = status ?: "unknown",
        createdAt = createdAt ?: "",
        totalRows = totalRows?.toString()?.toIntOrNull() ?: 0,
        processedRows = processedRows?.toString()?.toIntOrNull() ?: 0,
        failedRows = failedRows?.toString()?.toIntOrNull() ?: 0
    )
}

// Gson deserializes JSON numbers into Any-typed fields as Double, so "200".toIntOrNull()
// on the stringified value ("200.0") returns null. Handle both shapes explicitly.
private fun Any?.toIntSafe(): Int = when (this) {
    is Number -> toInt()
    is String -> toDoubleOrNull()?.toInt() ?: 0
    else -> 0
}

// ── GET /supplier/inventory DTOs ─────────────────────────────────────────────

data class InventoryResponseDto(
    @SerializedName("inventory") val inventory: InventoryPageDto? = null,
    @SerializedName("last_upload") val lastUpload: LastUploadDto? = null
)

data class InventoryPageDto(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("data") val data: List<InventoryItemDto>? = null,
    @SerializedName("last_page") val lastPage: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    @SerializedName("total") val total: Int? = null
)

data class InventoryItemDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("drug_name") val drugName: String? = null,
    @SerializedName("drug_name_raw") val drugNameRaw: String? = null,
    @SerializedName("is_catalog_matched") val isCatalogMatched: Boolean? = null,
    @SerializedName("quantity_available") val quantityAvailable: Any? = null,
    @SerializedName("unit_price") val unitPrice: String? = null,
    @SerializedName("public_price") val publicPrice: String? = null,
    @SerializedName("pharmacist_price") val pharmacistPrice: String? = null,
    @SerializedName("discount_pct") val discountPct: String? = null,
    // Only present in the PUT /supplier/inventory/{id} response, not the list endpoint —
    // fall back to computing it from unit_price/discount_pct when absent.
    @SerializedName("effective_price") val effectivePrice: Double? = null,
    @SerializedName("last_updated") val lastUpdated: String? = null,
    @SerializedName("drug") val drug: CatalogDrugDto? = null
) {
    fun toDomain(): InventoryItem {
        val price = unitPrice?.toDoubleOrNull() ?: 0.0
        val discount = discountPct?.toDoubleOrNull() ?: 0.0
        return InventoryItem(
            id = id?.toString() ?: "",
            drugName = drugName ?: "",
            drugNameRaw = drugNameRaw ?: drugName ?: "",
            isCatalogMatched = isCatalogMatched == true,
            quantityAvailable = quantityAvailable.toIntSafe(),
            unitPrice = price,
            publicPrice = publicPrice?.toDoubleOrNull() ?: 0.0,
            pharmacistPrice = pharmacistPrice?.toDoubleOrNull() ?: 0.0,
            discountPct = discount,
            effectivePrice = effectivePrice ?: (price * (1.0 - discount / 100.0)),
            lastUpdated = lastUpdated ?: "",
            catalogDrug = drug?.toDomain()
        )
    }
}

// Shared body shape for both POST supplier/inventory (create) and PUT supplier/inventory/{id} (update).
// The backend also requires public_price and pharmacist_price on top of unit_price, or it
// rejects the request with "Public price is required." / "Pharmacist price is required."
data class InventoryItemRequest(
    @SerializedName("drug_name_raw") val drugNameRaw: String,
    @SerializedName("quantity_available") val quantityAvailable: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("public_price") val publicPrice: Double,
    @SerializedName("pharmacist_price") val pharmacistPrice: Double,
    @SerializedName("discount_pct") val discountPct: Double
)

data class CatalogDrugDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("trade_name") val tradeName: String? = null,
    @SerializedName("dosage_form") val dosageForm: String? = null,
    @SerializedName("strength") val strength: String? = null
) {
    fun toDomain() = CatalogDrugInfo(
        id = id?.toString() ?: "",
        name = name ?: "",
        tradeName = tradeName ?: "",
        dosageForm = dosageForm ?: "",
        strength = strength ?: ""
    )
}

data class LastUploadDto(
    @SerializedName("upload_id") val uploadId: Any? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("uploaded_at") val uploadedAt: String? = null,
    @SerializedName("total_rows") val totalRows: Any? = null,
    @SerializedName("success_rows") val successRows: Any? = null,
    @SerializedName("failed_rows") val failedRows: Any? = null
) {
    fun toDomain() = UploadHistory(
        id = uploadId?.toString() ?: "",
        fileName = "",
        status = status ?: "unknown",
        createdAt = uploadedAt ?: "",
        totalRows = totalRows.toIntSafe(),
        processedRows = successRows.toIntSafe(),
        failedRows = failedRows.toIntSafe()
    )
}

data class CreateDrugRequest(
    @SerializedName("name") val name: String,
    @SerializedName("trade_name") val tradeName: String,
    @SerializedName("scientific_name") val scientificName: String,
    @SerializedName("manufacturer") val manufacturer: String,
    @SerializedName("dosage_form") val dosageForm: String,
    @SerializedName("strength") val strength: String,
    @SerializedName("barcode") val barcode: String? = null
)
