package com.pharmatrade.feature.admin.domain.model

// A drug created from a supplier's Arabic inventory sheet that has no English name yet — the
// admin adds one from the "Drugs missing English names" screen.
data class MissingEnglishDrug(
    val id: String,
    val nameAr: String,
    val createdAt: String
)

data class MissingEnglishDrugsPage(
    val items: List<MissingEnglishDrug>,
    val currentPage: Int,
    val lastPage: Int,
    val total: Int
) {
    val hasNextPage: Boolean get() = currentPage < lastPage
}

// What the admin fills in for one drug. Only englishName is required; blank optional fields are
// left out of the request.
data class DrugEnglishNameUpdate(
    val englishName: String,
    val scientificName: String = "",
    val manufacturer: String = "",
    val dosageForm: String = "",
    val strength: String = "",
    val barcode: String = ""
)

enum class DrugField { ENGLISH_NAME, SCIENTIFIC_NAME, MANUFACTURER, DOSAGE_FORM, STRENGTH, BARCODE }

enum class DrugFieldError { REQUIRED, ENGLISH_ONLY, TOO_LONG, ALREADY_TAKEN, INVALID }

// 422 from PUT admin/drugs/{id}, or local validation: which inputs to flag, and why.
class DrugValidationException(
    val fieldErrors: Map<DrugField, DrugFieldError>
) : Exception("Drug input is invalid: $fieldErrors")

// 404 from PUT admin/drugs/{id} — the drug was deleted since the list was loaded.
class DrugNotFoundException : Exception("Drug not found")

// 403 — the signed-in user isn't an admin.
class AdminForbiddenException : Exception("Forbidden")
