package com.pharmatrade.feature.admin.domain.usecase

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.domain.model.DrugEnglishNameUpdate
import com.pharmatrade.feature.admin.domain.model.DrugField
import com.pharmatrade.feature.admin.domain.model.DrugFieldError
import com.pharmatrade.feature.admin.domain.model.DrugValidationException
import com.pharmatrade.feature.admin.domain.repository.AdminRepository

class GetDrugsMissingEnglishUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke(page: Int = 1) = repository.getDrugsMissingEnglish(page)
}

class SaveDrugEnglishNameUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke(id: String, update: DrugEnglishNameUpdate): Result<Unit> {
        val trimmed = update.copy(
            englishName = update.englishName.trim(),
            scientificName = update.scientificName.trim(),
            manufacturer = update.manufacturer.trim(),
            dosageForm = update.dosageForm.trim(),
            strength = update.strength.trim(),
            barcode = update.barcode.trim()
        )
        val errors = validate(trimmed)
        if (errors.isNotEmpty()) return Result.Error("Drug input is invalid", DrugValidationException(errors))
        return repository.saveDrugEnglishName(id, trimmed)
    }

    // Same limits the backend enforces, checked first so the admin isn't sent a round trip away.
    private fun validate(u: DrugEnglishNameUpdate): Map<DrugField, DrugFieldError> = buildMap {
        when {
            u.englishName.isEmpty() -> put(DrugField.ENGLISH_NAME, DrugFieldError.REQUIRED)
            u.englishName.any { it.code > 0x7F } -> put(DrugField.ENGLISH_NAME, DrugFieldError.ENGLISH_ONLY)
            u.englishName.length > 255 -> put(DrugField.ENGLISH_NAME, DrugFieldError.TOO_LONG)
        }
        if (u.scientificName.length > 255) put(DrugField.SCIENTIFIC_NAME, DrugFieldError.TOO_LONG)
        if (u.manufacturer.length > 255) put(DrugField.MANUFACTURER, DrugFieldError.TOO_LONG)
        if (u.dosageForm.length > 100) put(DrugField.DOSAGE_FORM, DrugFieldError.TOO_LONG)
        if (u.strength.length > 100) put(DrugField.STRENGTH, DrugFieldError.TOO_LONG)
        if (u.barcode.length > 100) put(DrugField.BARCODE, DrugFieldError.TOO_LONG)
    }
}
