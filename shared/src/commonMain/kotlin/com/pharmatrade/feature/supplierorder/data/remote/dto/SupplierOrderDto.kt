package com.pharmatrade.feature.supplierorder.data.remote.dto

import com.pharmatrade.core.network.dto.rawDoubleOrZero
import com.pharmatrade.core.network.dto.rawIntOrZero
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderDetail
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderItem
import com.pharmatrade.feature.supplierorder.domain.model.SupplierOrderSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── GET /supplier/orders (paginated) ──────────────────────────────────────────

@Serializable
data class SupplierOrderPageDto(
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("data") val data: List<SupplierOrderSummaryDto>? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
    @SerialName("total") val total: Int? = null
)

@Serializable
data class SupplierOrderSummaryDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("master_order_id") val masterOrderId: JsonElement? = null,
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("subtotal") val subtotal: JsonElement? = null,
    @SerialName("commission_pct") val commissionPct: JsonElement? = null,
    @SerialName("commission_value") val commissionValue: JsonElement? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("master_order") val masterOrder: MasterOrderRefDto? = null
) {
    fun toDomain() = SupplierOrderSummary(
        id = id.rawIntOrZero().toString(),
        orderNumber = orderNumber ?: id.rawIntOrZero().toString(),
        status = status ?: "pending",
        subtotal = subtotal.rawDoubleOrZero(),
        commissionPct = commissionPct.rawDoubleOrZero(),
        commissionValue = commissionValue.rawDoubleOrZero(),
        createdAt = createdAt ?: "",
        pharmacyBranchName = masterOrder?.pharmacyBranch?.name ?: "",
        pharmacyBranchAddress = masterOrder?.pharmacyBranch?.address ?: "",
        pharmacyBranchPhone = masterOrder?.pharmacyBranch?.phone ?: ""
    )
}

@Serializable
data class MasterOrderRefDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("pharmacy_branch_id") val pharmacyBranchId: JsonElement? = null,
    @SerialName("pharmacy_branch") val pharmacyBranch: PharmacyBranchRefDto? = null
)

@Serializable
data class PharmacyBranchRefDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("phone") val phone: String? = null
)

// ── GET /supplier/orders/{id} ───────────────────────────────────────────────

@Serializable
data class SupplierOrderDetailDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("subtotal") val subtotal: JsonElement? = null,
    @SerialName("commission_pct") val commissionPct: JsonElement? = null,
    @SerialName("commission_value") val commissionValue: JsonElement? = null,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    @SerialName("shipped_at") val shippedAt: String? = null,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("pharmacy_branch") val pharmacyBranch: PharmacyBranchRefDto? = null,
    @SerialName("items") val items: List<SupplierOrderItemDto>? = null
) {
    fun toDomain() = SupplierOrderDetail(
        id = id.rawIntOrZero().toString(),
        orderNumber = orderNumber ?: id.rawIntOrZero().toString(),
        status = status ?: "pending",
        subtotal = subtotal.rawDoubleOrZero(),
        commissionPct = commissionPct.rawDoubleOrZero(),
        commissionValue = commissionValue.rawDoubleOrZero(),
        confirmedAt = confirmedAt,
        shippedAt = shippedAt,
        deliveredAt = deliveredAt,
        pharmacyBranchName = pharmacyBranch?.name ?: "",
        pharmacyBranchAddress = pharmacyBranch?.address ?: "",
        pharmacyBranchPhone = pharmacyBranch?.phone ?: "",
        items = items?.map { it.toDomain() } ?: emptyList()
    )
}

@Serializable
data class SupplierOrderItemDto(
    @SerialName("id") val id: JsonElement? = null,
    @SerialName("drug_name") val drugName: String? = null,
    @SerialName("dosage_form") val dosageForm: String? = null,
    @SerialName("strength") val strength: String? = null,
    @SerialName("quantity_requested") val quantityRequested: JsonElement? = null,
    @SerialName("quantity_confirmed") val quantityConfirmed: JsonElement? = null,
    @SerialName("unit_price") val unitPrice: JsonElement? = null,
    @SerialName("discount_pct") val discountPct: JsonElement? = null,
    @SerialName("line_total") val lineTotal: JsonElement? = null,
    @SerialName("status") val status: String? = null
) {
    fun toDomain() = SupplierOrderItem(
        id = id.rawIntOrZero().toString(),
        drugName = drugName ?: "",
        dosageForm = dosageForm,
        strength = strength,
        quantityRequested = quantityRequested.rawIntOrZero(),
        quantityConfirmed = quantityConfirmed?.let { it.rawIntOrZero() },
        unitPrice = unitPrice.rawDoubleOrZero(),
        discountPct = discountPct.rawDoubleOrZero(),
        lineTotal = lineTotal.rawDoubleOrZero(),
        status = status ?: ""
    )
}

// ── Request bodies ──────────────────────────────────────────────────────────

@Serializable
data class ConfirmOrderRequest(
    @SerialName("items") val items: List<ConfirmItemRequest>
)

@Serializable
data class ConfirmItemRequest(
    @SerialName("order_item_id") val orderItemId: Int,
    @SerialName("quantity_confirmed") val quantityConfirmed: Int
)

@Serializable
data class ReportShortageRequest(
    @SerialName("items") val items: List<ShortageItemRequest>
)

@Serializable
data class ShortageItemRequest(
    @SerialName("order_item_id") val orderItemId: Int,
    @SerialName("quantity_confirmed") val quantityConfirmed: Int,
    @SerialName("quantity_short") val quantityShort: Int,
    @SerialName("notes") val notes: String?
)
