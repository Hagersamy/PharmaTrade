package com.pharmatrade.feature.pharmacyorder.domain.model

data class SupplierCatalogPage(
    val items: List<SupplierCatalogItem>,
    val currentPage: Int,
    val lastPage: Int,
    val total: Int
)
