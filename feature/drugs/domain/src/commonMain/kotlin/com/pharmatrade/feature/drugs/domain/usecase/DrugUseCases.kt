package com.pharmatrade.feature.drugs.domain.usecase

import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.drugs.domain.model.InventoryData
import com.pharmatrade.feature.drugs.domain.model.InventoryItem
import com.pharmatrade.feature.drugs.domain.model.UploadHistory
import com.pharmatrade.feature.drugs.domain.repository.DrugRepository

class GetDrugsUseCase(private val repository: DrugRepository) {
    suspend operator fun invoke(
        search: String? = null,
        dosageForm: String? = null,
        perPage: Int? = null
    ): Result<List<Drug>> = repository.getDrugs(search = search, dosageForm = dosageForm, perPage = perPage)
}

class GetDrugDetailUseCase(private val repository: DrugRepository) {
    suspend operator fun invoke(id: String): Result<Drug> = repository.getDrugById(id)
}

class GetDosageFormsUseCase(private val repository: DrugRepository) {
    suspend operator fun invoke(): Result<List<String>> = repository.getDosageForms()
}

class UploadInventoryUseCase(private val repository: DrugRepository) {
    suspend operator fun invoke(fileUri: String, fileName: String): Result<UploadHistory> {
        if (fileUri.isBlank()) return Result.Error("No file selected")
        return repository.uploadInventory(fileUri, fileName)
    }
}

class GetUploadHistoryUseCase(private val repository: DrugRepository) {
    suspend operator fun invoke(): Result<List<UploadHistory>> = repository.getUploadHistory()
}

class GetInventoryUseCase(private val repository: DrugRepository) {
    suspend operator fun invoke(page: Int = 1): Result<InventoryData> = repository.getInventory(page)
}

class CreateSupplierDrugUseCase(private val repository: DrugRepository) {
    suspend operator fun invoke(
        name: String,
        tradeName: String,
        scientificName: String,
        manufacturer: String,
        dosageForm: String,
        strength: String,
        barcode: String? = null
    ): Result<Drug> {
        if (name.isBlank()) return Result.Error("Drug name is required")
        if (tradeName.isBlank()) return Result.Error("Trade name is required")
        return repository.createSupplierDrug(
            name = name,
            tradeName = tradeName,
            scientificName = scientificName,
            manufacturer = manufacturer,
            dosageForm = dosageForm,
            strength = strength,
            barcode = barcode
        )
    }
}

class CreateInventoryItemUseCase(private val repository: DrugRepository) {
    suspend operator fun invoke(
        drugNameRaw: String,
        quantityAvailable: Int,
        unitPrice: Double,
        discountPct: Double
    ): Result<InventoryItem> {
        if (drugNameRaw.isBlank()) return Result.Error("Select a drug")
        if (quantityAvailable <= 0) return Result.Error("Quantity must be greater than 0")
        if (unitPrice <= 0) return Result.Error("Price must be greater than 0")
        if (discountPct < 0 || discountPct >= 100) return Result.Error("Discount must be between 0 and 99%")
        return repository.createInventoryItem(drugNameRaw, quantityAvailable, unitPrice, discountPct)
    }
}

class UpdateInventoryItemUseCase(private val repository: DrugRepository) {
    suspend operator fun invoke(
        id: String,
        drugNameRaw: String,
        quantityAvailable: Int,
        unitPrice: Double,
        discountPct: Double
    ): Result<InventoryItem> {
        if (id.isBlank()) return Result.Error("Missing item id")
        if (drugNameRaw.isBlank()) return Result.Error("Drug name is required")
        if (quantityAvailable < 0) return Result.Error("Quantity cannot be negative")
        if (unitPrice < 0) return Result.Error("Price cannot be negative")
        if (discountPct < 0 || discountPct > 100) return Result.Error("Discount must be between 0 and 100")
        return repository.updateInventoryItem(id, drugNameRaw, quantityAvailable, unitPrice, discountPct)
    }
}
