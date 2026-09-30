package com.pharmatrade.feature.pharmacyorder.presentation.home

import com.pharmatrade.core.common.log.CrashReportSink
import com.pharmatrade.core.common.log.CrashReporter
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogPage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogDuplicateDiagnosticsTest {

    private val recorded = mutableListOf<Map<String, String>>()

    @BeforeTest
    fun setUp() {
        CrashReporter.sink = object : CrashReportSink {
            override fun log(message: String) = Unit
            override fun setKey(key: String, value: String) = Unit
            override fun recordNonFatal(error: Throwable, keys: Map<String, String>) {
                recorded += keys
            }
        }
    }

    @AfterTest
    fun tearDown() {
        CrashReporter.sink = null
    }

    private fun item(inventoryId: String, updated: String = "t1") = SupplierCatalogItem(
        id = "1_300", supplierId = "1", supplierName = "MedCo", drugId = "300", drugName = "CO TAREG",
        quantityAvailable = 1, unitPrice = 1.0, publicPrice = 1.0, pharmacistPrice = 1.0,
        discountPct = 0.0, effectivePrice = 1.0, inventoryId = inventoryId, lastUpdated = updated
    )

    private fun page(n: Int, total: Int, vararg items: SupplierCatalogItem) =
        SupplierCatalogPage(items.toList(), currentPage = n, lastPage = 68, total = total, hasNextPage = true)

    private fun causeOf(vararg pages: SupplierCatalogPage): String? {
        recorded.clear()
        val d = CatalogDuplicateDiagnostics()
        d.reset("")
        pages.forEach { d.onPage("", it.currentPage, it) }
        return recorded.singleOrNull()?.get("dup_cause")
    }

    @Test fun noDuplicate() = assertEquals(null, causeOf(page(1, 100, item("10")), page(2, 100)))
    @Test fun twoRowsSamePage() = assertEquals("BACKEND_DUPLICATE_ROWS_SAME_PAGE", causeOf(page(1, 100, item("10"), item("11"))))
    @Test fun sameRowSamePage() = assertEquals("BACKEND_SAME_ROW_TWICE_IN_ONE_RESPONSE", causeOf(page(1, 100, item("10"), item("10"))))
    @Test fun twoRowsAcrossPages() = assertEquals("BACKEND_DUPLICATE_ROWS_ACROSS_PAGES", causeOf(page(12, 100, item("10")), page(13, 100, item("11"))))
    @Test fun totalChanged() = assertEquals("DATA_CHANGED_WHILE_PAGING", causeOf(page(12, 100, item("10")), page(13, 101, item("10"))))
    @Test fun rowUpdated() = assertEquals("ROW_UPDATED_WHILE_PAGING", causeOf(page(12, 100, item("10", "t1")), page(13, 100, item("10", "t2"))))
    @Test fun unstableSort() = assertEquals("UNSTABLE_SORT_ORDER", causeOf(page(12, 100, item("10")), page(13, 100, item("10"))))

    @Test fun reportedOncePerKey() {
        recorded.clear()
        val d = CatalogDuplicateDiagnostics()
        d.reset("")
        d.onPage("", 1, page(1, 100, item("10")))
        d.onPage("", 2, page(2, 100, item("10")))
        d.onPage("", 3, page(3, 100, item("10")))
        assertEquals(1, recorded.size)
    }
}
