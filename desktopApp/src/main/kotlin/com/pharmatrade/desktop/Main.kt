package com.pharmatrade.desktop

import com.pharmatrade.feature.catalog.data.repository.CatalogRepositoryImpl
import com.pharmatrade.feature.catalog.domain.usecase.GetAllSellersUseCase
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val getAllSellers = GetAllSellersUseCase(CatalogRepositoryImpl())
    val result = getAllSellers()
    println("PharmaTrade desktop shell linked against :shared. Result: $result")
}
