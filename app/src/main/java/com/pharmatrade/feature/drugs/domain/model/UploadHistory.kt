package com.pharmatrade.feature.drugs.domain.model

data class UploadHistory(
    val id: String,
    val fileName: String,
    val status: String,
    val createdAt: String,
    val totalRows: Int,
    val processedRows: Int,
    val failedRows: Int
)
