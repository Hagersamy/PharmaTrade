package com.pharmatrade.core.common.model

data class Drug(
    val id: String,
    val name: String,
    val genericName: String,
    val category: DrugCategory,
    val manufacturer: String,
    val description: String,
    val dosageForm: String,
    val strength: String
)

enum class DrugCategory(val displayName: String) {
    ANALGESIC("Pain Relief"),
    ANTIBIOTIC("Antibiotics"),
    CARDIOVASCULAR("Cardiovascular"),
    DIABETES("Diabetes"),
    RESPIRATORY("Respiratory"),
    DIGESTIVE("Digestive"),
    VITAMINS("Vitamins & Supplements"),
    DERMATOLOGY("Dermatology"),
    PSYCHIATRIC("Psychiatric"),
    OTHER("Other")
}
