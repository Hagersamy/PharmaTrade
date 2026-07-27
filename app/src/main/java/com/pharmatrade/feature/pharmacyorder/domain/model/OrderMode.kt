package com.pharmatrade.feature.pharmacyorder.domain.model

enum class OrderMode(val apiValue: String) {
    BEST_DISCOUNT("best_discount"),
    SPECIFIC_SUPPLIER("specific_supplier")
}
