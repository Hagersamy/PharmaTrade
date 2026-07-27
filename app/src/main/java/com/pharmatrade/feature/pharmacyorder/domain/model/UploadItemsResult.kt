package com.pharmatrade.feature.pharmacyorder.domain.model

data class UploadItemsResult(
    val addedCount: Int,
    val skippedCount: Int = 0,
    val errors: List<String> = emptyList()
)
