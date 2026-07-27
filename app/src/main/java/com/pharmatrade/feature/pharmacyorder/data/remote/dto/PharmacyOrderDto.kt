package com.pharmatrade.feature.pharmacyorder.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.pharmatrade.feature.auth.data.remote.dto.ZoneDto
import com.pharmatrade.feature.pharmacyorder.domain.model.Branch
import com.pharmatrade.feature.pharmacyorder.domain.model.DraftOrderItem
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderDetail
import com.pharmatrade.feature.pharmacyorder.domain.model.OrderDrugLine
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacyOrderSummary
import com.pharmatrade.feature.pharmacyorder.domain.model.PharmacySupplier
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierInventoryItem
import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierOrder
import com.pharmatrade.feature.pharmacyorder.domain.model.UploadItemsResult

// Gson deserializes JSON numbers into Any-typed fields as Double, so a raw "5" from the
// backend can arrive as either a String or a Number depending on endpoint — handle both.
private fun Any?.toIntSafe(default: Int = 0): Int = when (this) {
    is Number -> toInt()
    is String -> toDoubleOrNull()?.toInt() ?: default
    else -> default
}

private fun Any?.toDoubleSafe(default: Double = 0.0): Double = when (this) {
    is Number -> toDouble()
    is String -> toDoubleOrNull() ?: default
    else -> default
}

// ── GET /pharmacy/branch ──────────────────────────────────────────────────────

data class BranchDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("zones") val zones: List<ZoneDto>? = null
) {
    fun toDomain() = Branch(
        id = id.toIntSafe().toString(),
        name = name ?: "",
        address = address ?: "",
        zones = zones?.map { it.toZone() } ?: emptyList()
    )
}

// ── GET /pharmacy/orders (paginated) ──────────────────────────────────────────

data class OrderPageDto(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("data") val data: List<OrderSummaryDto>? = null,
    @SerializedName("last_page") val lastPage: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    @SerializedName("total") val total: Int? = null
)

data class OrderSummaryDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("total_value") val totalValue: Any? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("supplier_count") val supplierCount: Any? = null,
    @SerializedName("supplier_orders") val supplierOrders: List<SupplierOrderDto>? = null
) {
    fun toDomain() = PharmacyOrderSummary(
        id = id.toIntSafe().toString(),
        orderNumber = orderNumber ?: id.toIntSafe().toString(),
        date = createdAt ?: "",
        totalValue = totalValue.toDoubleSafe(),
        status = status ?: "draft",
        supplierCount = supplierCount?.toIntSafe() ?: (supplierOrders?.size ?: 0)
    )
}

// ── GET /pharmacy/suppliers ───────────────────────────────────────────────────

data class SupplierDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("min_order_value") val minOrderValue: Any? = null,
    @SerializedName("min_order_qty") val minOrderQty: Any? = null,
    @SerializedName("zones") val zones: List<ZoneDto>? = null,
    @SerializedName("inventory_count") val inventoryCount: Any? = null,
    @SerializedName("last_inventory_update") val lastInventoryUpdate: String? = null
) {
    fun toDomain() = PharmacySupplier(
        id = id.toIntSafe().toString(),
        name = name ?: "",
        minOrderValue = minOrderValue.toDoubleSafe(),
        minOrderQty = minOrderQty.toIntSafe(),
        zones = zones?.map { it.toZone() } ?: emptyList(),
        inventoryCount = inventoryCount.toIntSafe(),
        lastInventoryUpdate = lastInventoryUpdate ?: ""
    )
}

// ── GET /pharmacy/suppliers/{id}/inventory ────────────────────────────────────

data class SupplierInventoryResponseDto(
    @SerializedName("supplier") val supplier: SupplierDto? = null,
    @SerializedName("inventory") val inventory: SupplierInventoryPageDto? = null
)

data class SupplierInventoryPageDto(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("data") val data: List<SupplierInventoryItemDto>? = null,
    @SerializedName("last_page") val lastPage: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    @SerializedName("total") val total: Int? = null
)

data class SupplierInventoryItemDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("drug_id") val drugId: Any? = null,
    @SerializedName("drug_name") val drugName: String? = null,
    @SerializedName("dosage_form") val dosageForm: String? = null,
    @SerializedName("strength") val strength: String? = null,
    @SerializedName("quantity_available") val quantityAvailable: Any? = null,
    @SerializedName("unit_price") val unitPrice: Any? = null,
    @SerializedName("discount_pct") val discountPct: Any? = null,
    @SerializedName("effective_price") val effectivePrice: Any? = null,
    // The sibling supplier-facing inventory endpoint (GET /supplier/inventory) nests the
    // catalog match under a "drug" object instead of a flat drug_id — fall back to it here
    // in case this endpoint follows the same convention, since a missing drug_id would make
    // every row default to the same id and collide in any per-drug UI state (quick-add
    // spinners, added-quantity badges, etc. all keyed by drugId).
    @SerializedName("drug") val drug: DrugRefDto? = null
) {
    fun toDomain(): SupplierInventoryItem {
        val price = unitPrice.toDoubleSafe()
        val discount = discountPct.toDoubleSafe()
        return SupplierInventoryItem(
            id = id.toIntSafe().toString(),
            drugId = (drugId ?: drug?.id).toIntSafe().toString(),
            drugName = drugName ?: "",
            dosageForm = dosageForm ?: drug?.dosageForm ?: "",
            strength = strength ?: drug?.strength ?: "",
            quantityAvailable = quantityAvailable.toIntSafe(),
            unitPrice = price,
            discountPct = discount,
            effectivePrice = effectivePrice?.toDoubleSafe() ?: (price * (1.0 - discount / 100.0))
        )
    }
}

data class DrugRefDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("dosage_form") val dosageForm: String? = null,
    @SerializedName("strength") val strength: String? = null
)

// ── GET /pharmacy/suppliers/drugs (paginated catalog across all suppliers) ───
// NOTE: response shape assumed — follows the same Laravel paginator convention and
// nested `supplier: {id, name}` object confirmed on other pharmacy-order endpoints,
// but no live sample of this specific endpoint was seen. Adjust field names if the
// real response differs.

data class SupplierCatalogPageDto(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("data") val data: List<SupplierCatalogItemDto>? = null,
    @SerializedName("last_page") val lastPage: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    @SerializedName("total") val total: Int? = null
)

data class SupplierCatalogItemDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("supplier") val supplier: SupplierRefDto? = null,
    @SerializedName("drug_id") val drugId: Any? = null,
    @SerializedName("drug_name") val drugName: String? = null,
    @SerializedName("dosage_form") val dosageForm: String? = null,
    @SerializedName("strength") val strength: String? = null,
    @SerializedName("quantity_available") val quantityAvailable: Any? = null,
    @SerializedName("unit_price") val unitPrice: Any? = null,
    @SerializedName("discount_pct") val discountPct: Any? = null,
    @SerializedName("effective_price") val effectivePrice: Any? = null,
    // See DrugRefDto note on SupplierInventoryItemDto — same fallback for a nested "drug" object.
    @SerializedName("drug") val drug: DrugRefDto? = null
) {
    fun toDomain(): SupplierCatalogItem {
        val price = unitPrice.toDoubleSafe()
        val discount = discountPct.toDoubleSafe()
        return SupplierCatalogItem(
            id = id.toIntSafe().toString(),
            supplierId = supplier?.id.toIntSafe().toString(),
            supplierName = supplier?.name ?: "",
            drugId = (drugId ?: drug?.id).toIntSafe().toString(),
            drugName = drugName ?: "",
            dosageForm = dosageForm ?: drug?.dosageForm ?: "",
            strength = strength ?: drug?.strength ?: "",
            quantityAvailable = quantityAvailable.toIntSafe(),
            unitPrice = price,
            discountPct = discount,
            effectivePrice = effectivePrice?.toDoubleSafe() ?: (price * (1.0 - discount / 100.0))
        )
    }
}

// ── Order detail (create / allocate / get-by-id all return this shape) ───────

data class OrderDetailDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("total_value") val totalValue: Any? = null,
    @SerializedName("savings") val savings: Any? = null,
    @SerializedName("discount_saved") val discountSaved: Any? = null,
    @SerializedName("items") val items: List<OrderItemDto>? = null,
    @SerializedName("supplier_orders") val supplierOrders: List<SupplierOrderDto>? = null,
    @SerializedName("has_shortage") val hasShortage: Boolean? = null,
    @SerializedName("shortage_items_count") val shortageItemsCount: Any? = null,
    // NOTE: field name assumed — no confirmed sample of a partially_available order's GET
    // response was seen yet. Adjust once a real shortage payload is observed.
    @SerializedName("shortage_reports") val shortageReports: List<ShortageReportDto>? = null
) {
    fun toDomain(): OrderDetail {
        val lines = supplierOrders?.map { it.toDomain() } ?: emptyList()
        val reportIds = shortageReports?.mapNotNull { it.id?.let { raw -> raw.toIntSafe().toString() } } ?: emptyList()
        return OrderDetail(
            id = id.toIntSafe().toString(),
            orderNumber = orderNumber ?: id.toIntSafe().toString(),
            date = createdAt ?: "",
            status = status ?: "draft",
            totalValue = totalValue.toDoubleSafe(),
            savings = (savings ?: discountSaved).toDoubleSafe(),
            items = items?.map { it.toDomain() } ?: emptyList(),
            supplierOrders = lines,
            hasUnresolvedShortage = hasShortage == true || status == "partially_available",
            shortageItemCount = shortageItemsCount?.toIntSafe() ?: reportIds.size,
            shortageReportIds = reportIds
        )
    }
}

data class ShortageReportDto(
    @SerializedName("id") val id: Any? = null
)

data class OrderItemDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("drug_id") val drugId: Any? = null,
    @SerializedName("drug_name") val drugName: String? = null,
    @SerializedName("quantity_requested") val quantityRequested: Any? = null
) {
    fun toDomain() = DraftOrderItem(
        id = id.toIntSafe().toString(),
        drugId = drugId.toIntSafe().toString(),
        drugName = drugName ?: "",
        quantity = quantityRequested.toIntSafe()
    )
}

data class SupplierRefDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null
)

data class SupplierOrderDto(
    @SerializedName("supplier") val supplier: SupplierRefDto? = null,
    @SerializedName("subtotal") val subtotal: Any? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("lines") val lines: List<DrugLineDto>? = null,
    @SerializedName("items") val items: List<DrugLineDto>? = null
) {
    fun toDomain() = SupplierOrder(
        supplierId = supplier?.id.toIntSafe().toString(),
        supplierName = supplier?.name ?: "",
        subtotal = subtotal.toDoubleSafe(),
        status = status ?: "",
        lines = (lines ?: items)?.map { it.toDomain() } ?: emptyList()
    )
}

data class DrugLineDto(
    @SerializedName("drug_name") val drugName: String? = null,
    @SerializedName("quantity_requested") val quantityRequested: Any? = null,
    @SerializedName("quantity_confirmed") val quantityConfirmed: Any? = null,
    @SerializedName("unit_price") val unitPrice: Any? = null,
    @SerializedName("discount_pct") val discountPct: Any? = null,
    @SerializedName("line_total") val lineTotal: Any? = null
) {
    fun toDomain() = OrderDrugLine(
        drugName = drugName ?: "",
        qtyRequested = quantityRequested.toIntSafe(),
        qtyConfirmed = quantityConfirmed?.let { it.toIntSafe() },
        unitPrice = unitPrice.toDoubleSafe(),
        lineTotal = lineTotal.toDoubleSafe(),
        discountPct = discountPct.toDoubleSafe()
    )
}

// ── POST /pharmacy/orders/{id}/upload ─────────────────────────────────────────

data class UploadItemsDto(
    @SerializedName("items_added") val itemsAdded: Any? = null,
    @SerializedName("items_skipped") val itemsSkipped: Any? = null,
    @SerializedName("errors") val errors: List<String>? = null
) {
    fun toDomain() = UploadItemsResult(
        addedCount = itemsAdded.toIntSafe(),
        skippedCount = itemsSkipped.toIntSafe(),
        errors = errors ?: emptyList()
    )
}

// ── Request bodies ────────────────────────────────────────────────────────────

data class CreateOrderRequest(
    @SerializedName("order_mode") val orderMode: String,
    @SerializedName("notes") val notes: String? = null
)

data class AddItemRequest(
    @SerializedName("drug_id") val drugId: String,
    @SerializedName("quantity") val quantity: Int
)

data class AllocateRequest(
    @SerializedName("supplier_id") val supplierId: String? = null
)

data class ResolveShortageRequest(
    @SerializedName("action") val action: String,
    @SerializedName("shortage_report_ids") val shortageReportIds: List<Int>
)
