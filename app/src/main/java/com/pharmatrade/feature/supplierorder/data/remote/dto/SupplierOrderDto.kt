package com.pharmatrade.feature.supplierorder.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderDetail
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderItem
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderSummary

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

// ── GET /supplier/orders (paginated) ──────────────────────────────────────────

data class SupplierOrderPageDto(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("data") val data: List<SupplierOrderSummaryDto>? = null,
    @SerializedName("last_page") val lastPage: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    @SerializedName("total") val total: Int? = null
)

data class SupplierOrderSummaryDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("master_order_id") val masterOrderId: Any? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("subtotal") val subtotal: Any? = null,
    @SerializedName("commission_pct") val commissionPct: Any? = null,
    @SerializedName("commission_value") val commissionValue: Any? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("master_order") val masterOrder: MasterOrderRefDto? = null
) {
    fun toDomain() = SupplierOrderSummary(
        id = id.toIntSafe().toString(),
        orderNumber = orderNumber ?: id.toIntSafe().toString(),
        status = status ?: "pending",
        subtotal = subtotal.toDoubleSafe(),
        commissionPct = commissionPct.toDoubleSafe(),
        commissionValue = commissionValue.toDoubleSafe(),
        createdAt = createdAt ?: "",
        pharmacyBranchName = masterOrder?.pharmacyBranch?.name ?: "",
        pharmacyBranchAddress = masterOrder?.pharmacyBranch?.address ?: "",
        pharmacyBranchPhone = masterOrder?.pharmacyBranch?.phone ?: ""
    )
}

data class MasterOrderRefDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("pharmacy_branch_id") val pharmacyBranchId: Any? = null,
    @SerializedName("pharmacy_branch") val pharmacyBranch: PharmacyBranchRefDto? = null
)

data class PharmacyBranchRefDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("phone") val phone: String? = null
)

// ── GET /supplier/orders/{id} ───────────────────────────────────────────────

data class SupplierOrderDetailDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("subtotal") val subtotal: Any? = null,
    @SerializedName("commission_pct") val commissionPct: Any? = null,
    @SerializedName("commission_value") val commissionValue: Any? = null,
    @SerializedName("confirmed_at") val confirmedAt: String? = null,
    @SerializedName("shipped_at") val shippedAt: String? = null,
    @SerializedName("delivered_at") val deliveredAt: String? = null,
    @SerializedName("pharmacy_branch") val pharmacyBranch: PharmacyBranchRefDto? = null,
    @SerializedName("items") val items: List<SupplierOrderItemDto>? = null
) {
    fun toDomain() = SupplierOrderDetail(
        id = id.toIntSafe().toString(),
        orderNumber = orderNumber ?: id.toIntSafe().toString(),
        status = status ?: "pending",
        subtotal = subtotal.toDoubleSafe(),
        commissionPct = commissionPct.toDoubleSafe(),
        commissionValue = commissionValue.toDoubleSafe(),
        confirmedAt = confirmedAt,
        shippedAt = shippedAt,
        deliveredAt = deliveredAt,
        pharmacyBranchName = pharmacyBranch?.name ?: "",
        pharmacyBranchAddress = pharmacyBranch?.address ?: "",
        pharmacyBranchPhone = pharmacyBranch?.phone ?: "",
        items = items?.map { it.toDomain() } ?: emptyList()
    )
}

data class SupplierOrderItemDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("drug_name") val drugName: String? = null,
    @SerializedName("dosage_form") val dosageForm: String? = null,
    @SerializedName("strength") val strength: String? = null,
    @SerializedName("quantity_requested") val quantityRequested: Any? = null,
    @SerializedName("quantity_confirmed") val quantityConfirmed: Any? = null,
    @SerializedName("unit_price") val unitPrice: Any? = null,
    @SerializedName("discount_pct") val discountPct: Any? = null,
    @SerializedName("line_total") val lineTotal: Any? = null,
    @SerializedName("status") val status: String? = null
) {
    fun toDomain() = SupplierOrderItem(
        id = id.toIntSafe().toString(),
        drugName = drugName ?: "",
        dosageForm = dosageForm,
        strength = strength,
        quantityRequested = quantityRequested.toIntSafe(),
        quantityConfirmed = quantityConfirmed?.toIntSafe(),
        unitPrice = unitPrice.toDoubleSafe(),
        discountPct = discountPct.toDoubleSafe(),
        lineTotal = lineTotal.toDoubleSafe(),
        status = status ?: ""
    )
}

// ── Request bodies ──────────────────────────────────────────────────────────

data class ConfirmOrderRequest(
    @SerializedName("items") val items: List<ConfirmItemRequest>
)

data class ConfirmItemRequest(
    @SerializedName("order_item_id") val orderItemId: Int,
    @SerializedName("quantity_confirmed") val quantityConfirmed: Int
)

data class ReportShortageRequest(
    @SerializedName("items") val items: List<ShortageItemRequest>
)

data class ShortageItemRequest(
    @SerializedName("order_item_id") val orderItemId: Int,
    @SerializedName("quantity_confirmed") val quantityConfirmed: Int,
    @SerializedName("quantity_short") val quantityShort: Int,
    @SerializedName("notes") val notes: String?
)
