package com.pharmatrade.feature.pharmacyorder.presentation.home

import com.pharmatrade.core.common.log.CrashReporter
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogPage
import kotlin.time.TimeMark
import kotlin.time.TimeSource

// Thrown only to be *recorded* (never propagated) — the message is what Crashlytics groups issues
// by, so each distinct cause shows up as its own issue in the console.
internal class CatalogDuplicateException(message: String) : Exception(message)

// Why duplicates happen is a backend question, but only the app sees the pages side by side the
// way a user scrolls them. This records every catalog page as a Crashlytics breadcrumb and, when
// the same supplierId_drugId key arrives twice (the thing that used to crash the LazyColumn with
// "Key ... was already used"), reports a non-fatal with both copies and a diagnosed cause.
// The list itself still drops the repeat (distinctBy in the ViewModel) — this only observes.
internal class CatalogDuplicateDiagnostics {

    private class Seen(
        val item: SupplierCatalogItem,
        val page: Int,
        val totalAtLoad: Int,
        val loadedAt: TimeMark
    )

    private var query: String? = null
    private val seen = mutableMapOf<String, Seen>()
    private val pageTotals = mutableListOf<Pair<Int, Int>>() // page -> total reported with it
    private val reported = mutableSetOf<String>()

    // A new list (new search, cleared search, retry) — pages restart at 1.
    fun reset(query: String) {
        this.query = query
        seen.clear()
        pageTotals.clear()
        reported.clear()
        CrashReporter.log("catalog: new list search=\"$query\"")
        CrashReporter.setKey("catalog_search", query)
    }

    fun onPage(query: String, requestedPage: Int, page: SupplierCatalogPage) {
        if (query != this.query) reset(query)
        val now = TimeSource.Monotonic.markNow()
        pageTotals += page.currentPage to page.total

        // One line per page: enough to rebuild what the user scrolled through, in order.
        // item format: supplierId_drugId#inventoryId
        CrashReporter.log(
            "catalog page=${page.currentPage} (requested $requestedPage) search=\"$query\" " +
                "items=${page.items.size} total=${page.total} last_page=${page.lastPage} " +
                "has_next=${page.hasNextPage} ids=" + page.items.joinToString(",") { "${it.id}#${it.inventoryId}" }
        )
        CrashReporter.setKey("catalog_pages_loaded", pageTotals.size.toString())
        CrashReporter.setKey("catalog_last_page", page.currentPage.toString())

        for (item in page.items) {
            val first = seen[item.id]
            if (first == null) {
                seen[item.id] = Seen(item, page.currentPage, page.total, now)
            } else {
                report(query, first, item, page)
            }
        }
    }

    private fun report(query: String, first: Seen, dup: SupplierCatalogItem, page: SupplierCatalogPage) {
        if (!reported.add(dup.id)) return
        val samePage = first.page == page.currentPage
        val sameRow = first.item.inventoryId == dup.inventoryId
        val totalChanged = first.totalAtLoad != page.total ||
            pageTotals.any { (p, total) -> p >= first.page && total != first.totalAtLoad }
        val rowUpdated = first.item.lastUpdated != dup.lastUpdated

        val cause = when {
            samePage && !sameRow -> "BACKEND_DUPLICATE_ROWS_SAME_PAGE"
            samePage -> "BACKEND_SAME_ROW_TWICE_IN_ONE_RESPONSE"
            !sameRow -> "BACKEND_DUPLICATE_ROWS_ACROSS_PAGES"
            totalChanged -> "DATA_CHANGED_WHILE_PAGING"
            rowUpdated -> "ROW_UPDATED_WHILE_PAGING"
            else -> "UNSTABLE_SORT_ORDER"
        }
        val explanation = when (cause) {
            "BACKEND_DUPLICATE_ROWS_SAME_PAGE" ->
                "Two different inventory rows for the same supplier+drug in one response."
            "BACKEND_SAME_ROW_TWICE_IN_ONE_RESPONSE" ->
                "The same inventory row appears twice in one response (likely a JOIN multiplying rows)."
            "BACKEND_DUPLICATE_ROWS_ACROSS_PAGES" ->
                "Two different inventory rows for the same supplier+drug, on different pages."
            "DATA_CHANGED_WHILE_PAGING" ->
                "Rows were added/removed between page loads (total changed), shifting offset pagination."
            "ROW_UPDATED_WHILE_PAGING" ->
                "The row was updated between page loads (last_updated differs) and moved in the sort order."
            else ->
                "Same row on two pages with no data change — ORDER BY has ties without a unique tiebreaker."
        }

        val keys = mapOf(
            "dup_cause" to cause,
            "dup_explanation" to explanation,
            "dup_key" to dup.id,
            "dup_drug_name" to dup.drugName,
            "dup_supplier" to "${dup.supplierId} ${dup.supplierName}",
            "dup_search" to query,
            "dup_first_page" to first.page.toString(),
            "dup_repeat_page" to page.currentPage.toString(),
            "dup_first_inventory_id" to first.item.inventoryId,
            "dup_repeat_inventory_id" to dup.inventoryId,
            "dup_first_last_updated" to first.item.lastUpdated,
            "dup_repeat_last_updated" to dup.lastUpdated,
            "dup_first_price" to first.item.effectivePrice.toString(),
            "dup_repeat_price" to dup.effectivePrice.toString(),
            "dup_total_at_first" to first.totalAtLoad.toString(),
            "dup_total_at_repeat" to page.total.toString(),
            "dup_seconds_between" to first.loadedAt.elapsedNow().inWholeSeconds.toString(),
            "dup_pages_loaded" to pageTotals.size.toString(),
            "dup_page_totals" to pageTotals.joinToString(",") { (p, t) -> "$p:$t" }.takeLast(900)
        )
        CrashReporter.log("catalog DUPLICATE ${dup.id} \"${dup.drugName}\" cause=$cause " + keys.entries.joinToString(" ") { "${it.key}=${it.value}" })
        CrashReporter.recordNonFatal(CatalogDuplicateException("Catalog duplicate: $cause"), keys)
    }
}
