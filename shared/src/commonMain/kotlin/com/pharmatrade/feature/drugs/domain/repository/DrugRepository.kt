package com.pharmatrade.feature.drugs.domain.repository

import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.drugs.domain.model.InventoryData
import com.pharmatrade.feature.drugs.domain.model.InventoryItem
import com.pharmatrade.feature.drugs.domain.model.UploadHistory

interface DrugRepository {
    suspend fun getDrugs(search: String? = null, dosageForm: String? = null, perPage: Int? = null): Result<List<Drug>>
    suspend fun getDrugById(id: String): Result<Drug>
    suspend fun getDosageForms(): Result<List<String>>
    suspend fun uploadInventory(fileUri: String, fileName: String): Result<UploadHistory>
    suspend fun getUploadHistory(): Result<List<UploadHistory>>
    suspend fun getInventory(page: Int = 1): Result<InventoryData>
    suspend fun createSupplierDrug(
        name: String,
        tradeName: String,
        scientificName: String,
        manufacturer: String,
        dosageForm: String,
        strength: String,
        barcode: String? = null
    ): Result<Drug>
    suspend fun createInventoryItem(
        drugNameRaw: String,
        quantityAvailable: Int,
        unitPrice: Double,
        discountPct: Double
    ): Result<InventoryItem>
    suspend fun updateInventoryItem(
        id: String,
        drugNameRaw: String,
        quantityAvailable: Int,
        unitPrice: Double,
        discountPct: Double
    ): Result<InventoryItem>
}
