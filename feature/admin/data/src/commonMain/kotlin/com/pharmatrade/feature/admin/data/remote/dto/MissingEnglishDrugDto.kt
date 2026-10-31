package com.pharmatrade.feature.admin.data.remote.dto

import com.pharmatrade.core.network.dto.rawIntOrZero
import com.pharmatrade.feature.admin.domain.model.DrugEnglishNameUpdate
import com.pharmatrade.feature.admin.domain.model.MissingEnglishDrug
import com.pharmatrade.feature.admin.domain.model.MissingEnglishDrugsPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── GET admin/drugs/missing-english ────────────────────────────────────────

@Serializable
data class MissingEnglishDrugsPageDto(
    @SerialName("data") val items: List<MissingEnglishDrugDto>? = null,
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("total") val total: Int? = null
) {
    fun toDomain(): MissingEnglishDrugsPage {
        val drugs = items.orEmpty().map { it.toDomain() }
        val page = currentPage ?: 1
        return MissingEnglishDrugsPage(
            items = drugs,
            currentPage = page,
            lastPage = lastPage ?: page,
            total = total ?: drugs.size
        )
    }
}

@Serializable
data class MissingEnglishDrugDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("commercial_name_ar") val commercialNameAr: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toDomain() = MissingEnglishDrug(
        id = id.rawIntOrZero().toString(),
        nameAr = commercialNameAr.orEmpty(),
        createdAt = createdAt.orEmpty()
    )
}

// ── PUT admin/drugs/{id} ───────────────────────────────────────────────────

// name and trade_name always carry the same English name. The optional fields are null when the
// admin left them blank — equal to their default, so the client's Json (encodeDefaults off) leaves
// them out instead of clearing whatever the drug already has.
@Serializable
data class UpdateDrugEnglishNameRequest(
    @SerialName("name") val name: String,
    @SerialName("trade_name") val tradeName: String,
    @SerialName("scientific_name") val scientificName: String? = null,
    @SerialName("manufacturer") val manufacturer: String? = null,
    @SerialName("dosage_form") val dosageForm: String? = null,
    @SerialName("strength") val strength: String? = null,
    @SerialName("barcode") val barcode: String? = null
)

fun DrugEnglishNameUpdate.toRequest() = UpdateDrugEnglishNameRequest(
    name = englishName,
    tradeName = englishName,
    scientificName = scientificName.ifBlank { null },
    manufacturer = manufacturer.ifBlank { null },
    dosageForm = dosageForm.ifBlank { null },
    strength = strength.ifBlank { null },
    barcode = barcode.ifBlank { null }
)
