package com.pharmatrade.feature.pharmacyorder.data.remote.dto

import com.pharmatrade.core.network.dto.rawDoubleOrZero
import com.pharmatrade.core.network.dto.rawIntOrZero
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── GET /pharmacy/branch ──────────────────────────────────────────────────────

@Serializable
data class BranchDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("zones") val zones: List<ZoneDto>? = null
) {
    fun toDomain() = Branch(
        id = id.rawIntOrZero().toString(),
        name = name ?: "",
        address = address ?: "",
        zones = zones?.map { it.toZone() } ?: emptyList()
    )
}

// ── GET /pharmacy/orders (paginated) ──────────────────────────────────────────

@Serializable
data class OrderPageDto(
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("data") val data: List<OrderSummaryDto>? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
    @SerialName("total") val total: Int? = null
)

@Serializable
data class OrderSummaryDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("total_value") val totalValue: JsonElement? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("supplier_count") val supplierCount: JsonElement? = null,
    @SerialName("supplier_orders") val supplierOrders: List<SupplierOrderDto>? = null
) {
    fun toDomain() = PharmacyOrderSummary(
        id = id.rawIntOrZero().toString(),
        orderNumber = orderNumber ?: id.rawIntOrZero().toString(),
        date = createdAt ?: "",
        totalValue = totalValue.rawDoubleOrZero(),
        status = status ?: "draft",
        supplierCount = supplierCount?.let { it.rawIntOrZero() } ?: (supplierOrders?.size ?: 0)
    )
}

// ── GET /pharmacy/suppliers ───────────────────────────────────────────────────

@Serializable
data class SupplierDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("min_order_value") val minOrderValue: JsonElement? = null,
    @SerialName("min_order_qty") val minOrderQty: JsonElement? = null,
    @SerialName("zones") val zones: List<ZoneDto>? = null,
    @SerialName("inventory_count") val inventoryCount: JsonElement? = null,
    @SerialName("last_inventory_update") val lastInventoryUpdate: String? = null
) {
    fun toDomain() = PharmacySupplier(
        id = id.rawIntOrZero().toString(),
        name = name ?: "",
        minOrderValue = minOrderValue.rawDoubleOrZero(),
        minOrderQty = minOrderQty.rawIntOrZero(),
        zones = zones?.map { it.toZone() } ?: emptyList(),
        inventoryCount = inventoryCount.rawIntOrZero(),
        lastInventoryUpdate = lastInventoryUpdate ?: ""
    )
}

// ── GET /pharmacy/suppliers/{id}/inventory ────────────────────────────────────

@Serializable
data class SupplierInventoryResponseDto(
    @SerialName("supplier") val supplier: SupplierDto? = null,
    @SerialName("inventory") val inventory: SupplierInventoryPageDto? = null
)

@Serializable
data class SupplierInventoryPageDto(
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("data") val data: List<SupplierInventoryItemDto>? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
    @SerialName("total") val total: Int? = null
)

@Serializable
data class SupplierInventoryItemDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("drug_id") val drugId: JsonElement? = null,
    @SerialName("drug_name") val drugName: String? = null,
    @SerialName("dosage_form") val dosageForm: String? = null,
    @SerialName("strength") val strength: String? = null,
    @SerialName("quantity_available") val quantityAvailable: JsonElement? = null,
    @SerialName("unit_price") val unitPrice: JsonElement? = null,
    @SerialName("discount_pct") val discountPct: JsonElement? = null,
    @SerialName("effective_price") val effectivePrice: JsonElement? = null,
    // The sibling supplier-facing inventory endpoint (GET /supplier/inventory) nests the
    // catalog match under a "drug" object instead of a flat drug_id — fall back to it here
    // in case this endpoint follows the same convention, since a missing drug_id would make
    // every row default to the same id and collide in any per-drug UI state (quick-add
    // spinners, added-quantity badges, etc. all keyed by drugId).
    @SerialName("drug") val drug: DrugRefDto? = null
) {
    fun toDomain(): SupplierInventoryItem {
        val price = unitPrice.rawDoubleOrZero()
        val discount = discountPct.rawDoubleOrZero()
        return SupplierInventoryItem(
            id = id.rawIntOrZero().toString(),
            drugId = (drugId ?: drug?.id).rawIntOrZero().toString(),
            drugName = drugName ?: "",
            dosageForm = dosageForm ?: drug?.dosageForm ?: "",
            strength = strength ?: drug?.strength ?: "",
            quantityAvailable = quantityAvailable.rawIntOrZero(),
            unitPrice = price,
            discountPct = discount,
            effectivePrice = effectivePrice?.let { it.rawDoubleOrZero() } ?: (price * (1.0 - discount / 100.0))
        )
    }
}

@Serializable
data class DrugRefDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("dosage_form") val dosageForm: String? = null,
    @SerialName("strength") val strength: String? = null
)

// ── GET /pharmacy/suppliers/drugs (paginated catalog across all suppliers) ───
// Confirmed live response shape (2026-09-11): items are keyed by "inventory_id", not "id", and
// also carry public_price/pharmacist_price alongside unit_price/effective_price, plus a few
// fields we don't use yet (drug_name_raw, scientific_name, savings_per_unit, order_limit).
// ignoreUnknownKeys=true (ApiClient) means those extras are safely dropped.

@Serializable
data class SupplierCatalogPageDto(
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("data") val data: List<SupplierCatalogItemDto>? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
    @SerialName("total") val total: Int? = null
)

@Serializable
data class SupplierCatalogItemDto(
    @SerialName("inventory_id") val inventoryId: JsonElement? = null,
    @SerialName("supplier") val supplier: SupplierRefDto? = null,
    @SerialName("drug_id") val drugId: JsonElement? = null,
    @SerialName("drug_name") val drugName: String? = null,
    @SerialName("dosage_form") val dosageForm: String? = null,
    @SerialName("strength") val strength: String? = null,
    @SerialName("quantity_available") val quantityAvailable: JsonElement? = null,
    @SerialName("unit_price") val unitPrice: JsonElement? = null,
    @SerialName("public_price") val publicPrice: JsonElement? = null,
    @SerialName("pharmacist_price") val pharmacistPrice: JsonElement? = null,
    @SerialName("discount_pct") val discountPct: JsonElement? = null,
    @SerialName("effective_price") val effectivePrice: JsonElement? = null,
    // See DrugRefDto note on SupplierInventoryItemDto — same fallback for a nested "drug" object.
    @SerialName("drug") val drug: DrugRefDto? = null
) {
    fun toDomain(): SupplierCatalogItem {
        val price = unitPrice.rawDoubleOrZero()
        val discount = discountPct.rawDoubleOrZero()
        return SupplierCatalogItem(
            id = inventoryId.rawIntOrZero().toString(),
            supplierId = supplier?.id.rawIntOrZero().toString(),
            supplierName = supplier?.name ?: "",
            drugId = (drugId ?: drug?.id).rawIntOrZero().toString(),
            drugName = drugName ?: "",
            dosageForm = dosageForm ?: drug?.dosageForm ?: "",
            strength = strength ?: drug?.strength ?: "",
            quantityAvailable = quantityAvailable.rawIntOrZero(),
            unitPrice = price,
            publicPrice = publicPrice.rawDoubleOrZero(),
            pharmacistPrice = pharmacistPrice.rawDoubleOrZero(),
            discountPct = discount,
            effectivePrice = effectivePrice?.let { it.rawDoubleOrZero() } ?: (price * (1.0 - discount / 100.0))
        )
    }
}

// ── Order detail (create / allocate / get-by-id all return this shape) ───────

@Serializable
data class OrderDetailDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("total_value") val totalValue: JsonElement? = null,
    @SerialName("savings") val savings: JsonElement? = null,
    @SerialName("discount_saved") val discountSaved: JsonElement? = null,
    @SerialName("items") val items: List<OrderItemDto>? = null,
    @SerialName("supplier_orders") val supplierOrders: List<SupplierOrderDto>? = null,
    @SerialName("has_shortage") val hasShortage: Boolean? = null,
    @SerialName("shortage_items_count") val shortageItemsCount: JsonElement? = null,
    // NOTE: field name assumed — no confirmed sample of a partially_available order's GET
    // response was seen yet. Adjust once a real shortage payload is observed.
    @SerialName("shortage_reports") val shortageReports: List<ShortageReportDto>? = null
) {
    fun toDomain(): OrderDetail {
        val lines = supplierOrders?.map { it.toDomain() } ?: emptyList()
        val reportIds = shortageReports?.mapNotNull { it.id?.let { raw -> raw.rawIntOrZero().toString() } } ?: emptyList()
        return OrderDetail(
            id = id.rawIntOrZero().toString(),
            orderNumber = orderNumber ?: id.rawIntOrZero().toString(),
            date = createdAt ?: "",
            status = status ?: "draft",
            totalValue = totalValue.rawDoubleOrZero(),
            savings = (savings ?: discountSaved).rawDoubleOrZero(),
            items = items?.map { it.toDomain() } ?: emptyList(),
            supplierOrders = lines,
            hasUnresolvedShortage = hasShortage == true || status == "partially_available",
            shortageItemCount = shortageItemsCount?.let { it.rawIntOrZero() } ?: reportIds.size,
            shortageReportIds = reportIds
        )
    }
}

@Serializable
data class ShortageReportDto(
    @SerialName("id") val id: JsonElement? = null
)

@Serializable
data class OrderItemDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("drug_id") val drugId: JsonElement? = null,
    @SerialName("drug_name") val drugName: String? = null,
    @SerialName("quantity_requested") val quantityRequested: JsonElement? = null
) {
    fun toDomain() = DraftOrderItem(
        id = id.rawIntOrZero().toString(),
        drugId = drugId.rawIntOrZero().toString(),
        drugName = drugName ?: "",
        quantity = quantityRequested.rawIntOrZero()
    )
}

@Serializable
data class SupplierRefDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null
)

@Serializable
data class SupplierOrderDto(
    @SerialName("supplier") val supplier: SupplierRefDto? = null,
    @SerialName("subtotal") val subtotal: JsonElement? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("lines") val lines: List<DrugLineDto>? = null,
    @SerialName("items") val items: List<DrugLineDto>? = null
) {
    fun toDomain() = SupplierOrder(
        supplierId = supplier?.id.rawIntOrZero().toString(),
        supplierName = supplier?.name ?: "",
        subtotal = subtotal.rawDoubleOrZero(),
        status = status ?: "",
        lines = (lines ?: items)?.map { it.toDomain() } ?: emptyList()
    )
}

@Serializable
data class DrugLineDto(
    @SerialName("drug_name") val drugName: String? = null,
    @SerialName("quantity_requested") val quantityRequested: JsonElement? = null,
    @SerialName("quantity_confirmed") val quantityConfirmed: JsonElement? = null,
    @SerialName("unit_price") val unitPrice: JsonElement? = null,
    @SerialName("discount_pct") val discountPct: JsonElement? = null,
    @SerialName("line_total") val lineTotal: JsonElement? = null
) {
    fun toDomain() = OrderDrugLine(
        drugName = drugName ?: "",
        qtyRequested = quantityRequested.rawIntOrZero(),
        qtyConfirmed = quantityConfirmed?.let { it.rawIntOrZero() },
        unitPrice = unitPrice.rawDoubleOrZero(),
        lineTotal = lineTotal.rawDoubleOrZero(),
        discountPct = discountPct.rawDoubleOrZero()
    )
}

// ── POST /pharmacy/orders/{id}/upload ─────────────────────────────────────────

@Serializable
data class UploadItemsDto(
    @SerialName("items_added") val itemsAdded: JsonElement? = null,
    @SerialName("items_skipped") val itemsSkipped: JsonElement? = null,
    @SerialName("errors") val errors: List<String>? = null
) {
    fun toDomain() = UploadItemsResult(
        addedCount = itemsAdded.rawIntOrZero(),
        skippedCount = itemsSkipped.rawIntOrZero(),
        errors = errors ?: emptyList()
    )
}

// ── Request bodies ────────────────────────────────────────────────────────────

@Serializable
data class CreateOrderRequest(
    @SerialName("order_mode") val orderMode: String,
    @SerialName("notes") val notes: String? = null
)

@Serializable
data class AddItemRequest(
    @SerialName("drug_id") val drugId: String,
    @SerialName("quantity") val quantity: Int
)

@Serializable
data class AllocateRequest(
    @SerialName("supplier_id") val supplierId: String? = null
)

@Serializable
data class ResolveShortageRequest(
    @SerialName("action") val action: String,
    @SerialName("shortage_report_ids") val shortageReportIds: List<Int>
)
