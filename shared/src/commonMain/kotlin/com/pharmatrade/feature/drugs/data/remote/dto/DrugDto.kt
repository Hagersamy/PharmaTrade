package com.pharmatrade.feature.drugs.data.remote.dto

import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.model.DrugCategory
import com.pharmatrade.core.network.dto.rawIntOrZero
import com.pharmatrade.core.network.dto.rawStringOrNull
import com.pharmatrade.feature.drugs.domain.model.CatalogDrugInfo
import com.pharmatrade.feature.drugs.domain.model.InventoryItem
import com.pharmatrade.feature.drugs.domain.model.UploadHistory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DrugDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("trade_name") val tradeName: String? = null,
    @SerialName("scientific_name") val scientificName: String? = null,
    @SerialName("manufacturer") val manufacturer: String? = null,
    @SerialName("dosage_form") val dosageForm: String? = null,
    @SerialName("strength") val strength: String? = null,
    @SerialName("barcode") val barcode: String? = null
) {
    fun toDomain(): Drug = Drug(
        id = id.rawStringOrNull() ?: "",
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
@Serializable
data class DrugPageDto(
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("data") val data: List<DrugDto>? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
    @SerialName("total") val total: Int? = null
)

@Serializable
data class UploadHistoryDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("total_rows") val totalRows: JsonElement? = null,
    @SerialName("processed_rows") val processedRows: JsonElement? = null,
    @SerialName("failed_rows") val failedRows: JsonElement? = null
) {
    fun toDomain() = UploadHistory(
        id = id.rawStringOrNull() ?: "",
        fileName = fileName ?: "",
        status = status ?: "unknown",
        createdAt = createdAt ?: "",
        totalRows = totalRows.rawIntOrZero(),
        processedRows = processedRows.rawIntOrZero(),
        failedRows = failedRows.rawIntOrZero()
    )
}

// ── GET /supplier/inventory DTOs ─────────────────────────────────────────────

@Serializable
data class InventoryResponseDto(
    @SerialName("inventory") val inventory: InventoryPageDto? = null,
    @SerialName("last_upload") val lastUpload: LastUploadDto? = null
)

@Serializable
data class InventoryPageDto(
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("data") val data: List<InventoryItemDto>? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
    @SerialName("total") val total: Int? = null
)

@Serializable
data class InventoryItemDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("drug_name") val drugName: String? = null,
    @SerialName("drug_name_raw") val drugNameRaw: String? = null,
    @SerialName("is_catalog_matched") val isCatalogMatched: Boolean? = null,
    @SerialName("quantity_available") val quantityAvailable: JsonElement? = null,
    @SerialName("unit_price") val unitPrice: String? = null,
    @SerialName("public_price") val publicPrice: String? = null,
    @SerialName("pharmacist_price") val pharmacistPrice: String? = null,
    @SerialName("discount_pct") val discountPct: String? = null,
    // Only present in the PUT /supplier/inventory/{id} response, not the list endpoint —
    // fall back to computing it from unit_price/discount_pct when absent.
    @SerialName("effective_price") val effectivePrice: Double? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
    @SerialName("drug") val drug: CatalogDrugDto? = null
) {
    fun toDomain(): InventoryItem {
        val price = unitPrice?.toDoubleOrNull() ?: 0.0
        val discount = discountPct?.toDoubleOrNull() ?: 0.0
        return InventoryItem(
            id = id.rawStringOrNull() ?: "",
            drugName = drugName ?: "",
            drugNameRaw = drugNameRaw ?: drugName ?: "",
            isCatalogMatched = isCatalogMatched == true,
            quantityAvailable = quantityAvailable.rawIntOrZero(),
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
@Serializable
data class InventoryItemRequest(
    @SerialName("drug_name_raw") val drugNameRaw: String,
    @SerialName("quantity_available") val quantityAvailable: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("public_price") val publicPrice: Double,
    @SerialName("pharmacist_price") val pharmacistPrice: Double,
    @SerialName("discount_pct") val discountPct: Double
)

@Serializable
data class CatalogDrugDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("trade_name") val tradeName: String? = null,
    @SerialName("dosage_form") val dosageForm: String? = null,
    @SerialName("strength") val strength: String? = null
) {
    fun toDomain() = CatalogDrugInfo(
        id = id.rawStringOrNull() ?: "",
        name = name ?: "",
        tradeName = tradeName ?: "",
        dosageForm = dosageForm ?: "",
        strength = strength ?: ""
    )
}

@Serializable
data class LastUploadDto(
    @SerialName("upload_id") val uploadId: JsonElement? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
    @SerialName("total_rows") val totalRows: JsonElement? = null,
    @SerialName("success_rows") val successRows: JsonElement? = null,
    @SerialName("failed_rows") val failedRows: JsonElement? = null
) {
    fun toDomain() = UploadHistory(
        id = uploadId.rawStringOrNull() ?: "",
        fileName = "",
        status = status ?: "unknown",
        createdAt = uploadedAt ?: "",
        totalRows = totalRows.rawIntOrZero(),
        processedRows = successRows.rawIntOrZero(),
        failedRows = failedRows.rawIntOrZero()
    )
}

@Serializable
data class CreateDrugRequest(
    @SerialName("name") val name: String,
    @SerialName("trade_name") val tradeName: String,
    @SerialName("scientific_name") val scientificName: String,
    @SerialName("manufacturer") val manufacturer: String,
    @SerialName("dosage_form") val dosageForm: String,
    @SerialName("strength") val strength: String,
    @SerialName("barcode") val barcode: String? = null
)
