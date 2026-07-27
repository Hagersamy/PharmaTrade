package com.pharmatrade.core.network

data class FormFile(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String
)
