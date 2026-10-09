package com.pharmatrade.feature.pharmacyorder.data.local

import com.pharmatrade.feature.pharmacyorder.domain.model.SupplierCatalogItem
import com.pharmatrade.feature.pharmacyorder.domain.repository.PharmacyCartLine
import com.pharmatrade.feature.pharmacyorder.domain.repository.PharmacyCartSnapshot
import com.pharmatrade.feature.pharmacyorder.domain.repository.PharmacyCartStore
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// One JSON entry per logged-in user, so a different pharmacy on the same phone never sees it.
class PharmacyCartStoreImpl(private val settings: Settings) : PharmacyCartStore {

    private val json = Json { ignoreUnknownKeys = true }

    override fun load(ownerId: String): PharmacyCartSnapshot? {
        val stored = settings.getStringOrNull(key(ownerId)) ?: return null
        return runCatching { json.decodeFromString(StoredCart.serializer(), stored) }.getOrNull()?.toDomain()
    }

    override fun save(ownerId: String, snapshot: PharmacyCartSnapshot) {
        if (snapshot.lines.isEmpty() && snapshot.orderIdsBySupplier.isEmpty()) {
            settings.remove(key(ownerId))
        } else {
            settings.putString(key(ownerId), json.encodeToString(StoredCart.serializer(), StoredCart.from(snapshot)))
        }
    }

    private fun key(ownerId: String) = "pharmacy_cart_$ownerId"
}

@Serializable
private data class StoredCart(
    val orderIdsBySupplier: Map<String, String>,
    val lines: List<StoredLine>
) {
    fun toDomain() = PharmacyCartSnapshot(orderIdsBySupplier, lines.map { it.toDomain() })

    companion object {
        fun from(snapshot: PharmacyCartSnapshot) =
            StoredCart(snapshot.orderIdsBySupplier, snapshot.lines.map { StoredLine.from(it) })
    }
}

@Serializable
private data class StoredLine(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val drugId: String,
    val drugName: String,
    val dosageForm: String = "",
    val strength: String = "",
    val quantityAvailable: Int,
    val unitPrice: Double,
    val publicPrice: Double,
    val pharmacistPrice: Double,
    val discountPct: Double,
    val effectivePrice: Double,
    val inventoryId: String = "",
    val quantity: Int,
    val syncedQuantity: Int,
    val backendItemId: String? = null
) {
    fun toDomain() = PharmacyCartLine(
        item = SupplierCatalogItem(
            id = id, supplierId = supplierId, supplierName = supplierName, drugId = drugId,
            drugName = drugName, dosageForm = dosageForm, strength = strength,
            quantityAvailable = quantityAvailable, unitPrice = unitPrice, publicPrice = publicPrice,
            pharmacistPrice = pharmacistPrice, discountPct = discountPct,
            effectivePrice = effectivePrice, inventoryId = inventoryId
        ),
        quantity = quantity,
        syncedQuantity = syncedQuantity,
        backendItemId = backendItemId
    )

    companion object {
        fun from(line: PharmacyCartLine) = with(line.item) {
            StoredLine(
                id = id, supplierId = supplierId, supplierName = supplierName, drugId = drugId,
                drugName = drugName, dosageForm = dosageForm, strength = strength,
                quantityAvailable = quantityAvailable, unitPrice = unitPrice, publicPrice = publicPrice,
                pharmacistPrice = pharmacistPrice, discountPct = discountPct,
                effectivePrice = effectivePrice, inventoryId = inventoryId,
                quantity = line.quantity, syncedQuantity = line.syncedQuantity,
                backendItemId = line.backendItemId
            )
        }
    }
}
