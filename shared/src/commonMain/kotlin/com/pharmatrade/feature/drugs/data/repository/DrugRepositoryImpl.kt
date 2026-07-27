package com.pharmatrade.feature.drugs.data.repository

import com.pharmatrade.core.common.model.Drug
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.core.io.PlatformFileReader
import com.pharmatrade.core.network.FormFile
import com.pharmatrade.feature.drugs.data.remote.DrugApi
import com.pharmatrade.feature.drugs.data.remote.dto.CreateDrugRequest
import com.pharmatrade.feature.drugs.data.remote.dto.InventoryItemRequest
import com.pharmatrade.feature.drugs.domain.model.InventoryData
import com.pharmatrade.feature.drugs.domain.model.InventoryItem
import com.pharmatrade.feature.drugs.domain.model.UploadHistory
import com.pharmatrade.feature.drugs.domain.repository.DrugRepository
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DrugRepositoryImpl(
    private val fileReader: PlatformFileReader,
    private val api: DrugApi = DrugApi()
) : DrugRepository {

    override suspend fun getDrugs(search: String?, dosageForm: String?, perPage: Int?): Result<List<Drug>> = try {
        val response = api.getDrugs(search = search, dosageForm = dosageForm, perPage = perPage)
        println("$TAG: getDrugs(search=$search, dosageForm=$dosageForm, perPage=$perPage) raw response: ${response.data}")
        val drugs = response.data?.data?.map { it.toDomain() } ?: emptyList()
        println("$TAG: getDrugs mapped ${drugs.size} drugs: $drugs")
        Result.Success(drugs)
    } catch (e: ResponseException) {
        println("$TAG: getDrugs failed: HTTP ${e.response.status.value} | $e")
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        println("$TAG: getDrugs failed: ${e.message} | $e")
        Result.Error(e.message ?: "Failed to load drugs", e)
    }

    override suspend fun getDrugById(id: String): Result<Drug> = try {
        val response = api.getDrugById(id)
        val drug = response.data?.toDomain()
            ?: return Result.Error("Drug not found")
        Result.Success(drug)
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load drug details", e)
    }

    override suspend fun getDosageForms(): Result<List<String>> = try {
        val response = api.getDosageForms()
        Result.Success(response.data ?: emptyList())
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load dosage forms", e)
    }

    override suspend fun uploadInventory(fileUri: String, fileName: String): Result<UploadHistory> = try {
        val info = fileReader.read(fileUri) ?: return Result.Error("Could not read the selected file.")
        val resolvedName = info.displayName.ifBlank { fileName }

        val ext = resolvedName.substringAfterLast('.', "").lowercase()
        val mime = when (ext) {
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls"  -> "application/vnd.ms-excel"
            "csv"  -> "text/csv"
            else   -> info.mimeType ?: "application/octet-stream"
        }

        val response = api.uploadInventory(FormFile(bytes = info.bytes, fileName = resolvedName, mimeType = mime))
        val history = response.data?.toDomain()
            ?: UploadHistory("", resolvedName, "success", "", 0, 0, 0)
        Result.Success(history)
    } catch (e: ResponseException) {
        val errorBody = runCatching { e.response.bodyAsText() }.getOrNull()
        Result.Error(parseHttpError(e.response.status.value, errorBody))
    } catch (_: SocketTimeoutException) {
        Result.Error("Connection timed out. Make sure the server is running.")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Upload failed")
    }

    private fun parseHttpError(code: Int, body: String?): String {
        if (body.isNullOrBlank()) return "Server error ($code)"
        return runCatching {
            val json = Json.parseToJsonElement(body).jsonObject
            // Laravel returns { message: "...", errors: { field: [...] } }
            val message = json["message"]?.jsonPrimitive?.contentOrNull
            val errors = json["errors"]?.jsonObject
            if (errors != null && errors.isNotEmpty()) {
                val fieldMsg = errors.values.firstOrNull()?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                "Validation error: $fieldMsg"
            } else {
                message ?: "Server error ($code)"
            }
        }.getOrDefault("Server error ($code)")
    }

    override suspend fun getUploadHistory(): Result<List<UploadHistory>> = try {
        val response = api.getUploadHistory()
        Result.Success(response.data?.map { it.toDomain() } ?: emptyList())
    } catch (e: ResponseException) {
        Result.Error("Server error (${e.response.status.value})")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load upload history")
    }

    override suspend fun getInventory(page: Int): Result<InventoryData> = try {
        // NOTE: sending a per_page override on this endpoint was found to change what the
        // backend returns — instead of just resizing the page, it started scoping results to
        // only the latest upload. So we never send per_page here; we only ever use ?page=N at
        // the backend's own default page size, and let the UI page through as the user scrolls.
        val dto = api.getInventory(page = page).data
        val rawItems = dto?.inventory?.data ?: emptyList()
        val items = rawItems.map { it.toDomain() }
        val total = dto?.inventory?.total ?: items.size
        val lastUpload = dto?.lastUpload?.toDomain()
        val currentPage = dto?.inventory?.currentPage ?: page
        val lastPage = dto?.inventory?.lastPage ?: currentPage

        println(
            "$TAG: page $currentPage/$lastPage: per_page=${dto?.inventory?.perPage}, total=$total, items_in_page=${items.size}"
        )

        // Log raw backend fields next to the mapped domain values per drug, so a mismatch
        // between "what the backend sent" and "what Home displays" is visible in the console
        // instead of having to diff the full HTTP body dump by hand.
        rawItems.zip(items).forEach { (raw, mapped) ->
            println(
                "$TAG: item raw: id=${raw.id} drug_name=${raw.drugName} drug_name_raw=${raw.drugNameRaw} " +
                    "is_catalog_matched=${raw.isCatalogMatched} quantity_available=${raw.quantityAvailable} " +
                    "unit_price=${raw.unitPrice} discount_pct=${raw.discountPct} effective_price=${raw.effectivePrice} " +
                    "drug.id=${raw.drug?.id} drug.name=${raw.drug?.name} drug.trade_name=${raw.drug?.tradeName} " +
                    "|| mapped: id=${mapped.id} drugName=${mapped.drugName} drugNameRaw=${mapped.drugNameRaw} " +
                    "quantityAvailable=${mapped.quantityAvailable} unitPrice=${mapped.unitPrice} " +
                    "discountPct=${mapped.discountPct} effectivePrice=${mapped.effectivePrice}"
            )
        }

        Result.Success(
            InventoryData(
                items = items,
                totalItems = total,
                currentPage = currentPage,
                lastPage = lastPage,
                lastUpload = lastUpload
            )
        )
    } catch (e: ResponseException) {
        println("$TAG: getInventory failed: HTTP ${e.response.status.value} | $e")
        Result.Error("Server error (${e.response.status.value})")
    } catch (e: Exception) {
        println("$TAG: getInventory failed: ${e.message} | $e")
        Result.Error(e.message ?: "Failed to load inventory")
    }

    override suspend fun createSupplierDrug(
        name: String,
        tradeName: String,
        scientificName: String,
        manufacturer: String,
        dosageForm: String,
        strength: String,
        barcode: String?
    ): Result<Drug> = try {
        val request = CreateDrugRequest(
            name = name,
            tradeName = tradeName,
            scientificName = scientificName,
            manufacturer = manufacturer,
            dosageForm = dosageForm,
            strength = strength,
            barcode = barcode?.takeIf { it.isNotBlank() }
        )
        val response = api.createSupplierDrug(request)
        val drug = response.data?.toDomain() ?: return Result.Error("Server did not return the created drug")
        Result.Success(drug)
    } catch (e: ResponseException) {
        val errorBody = runCatching { e.response.bodyAsText() }.getOrNull()
        Result.Error(parseHttpError(e.response.status.value, errorBody))
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to add drug")
    }

    override suspend fun createInventoryItem(
        drugNameRaw: String,
        quantityAvailable: Int,
        unitPrice: Double,
        discountPct: Double
    ): Result<InventoryItem> = try {
        val request = InventoryItemRequest(
            drugNameRaw = drugNameRaw,
            quantityAvailable = quantityAvailable,
            unitPrice = unitPrice,
            publicPrice = unitPrice,
            pharmacistPrice = unitPrice,
            discountPct = discountPct
        )
        val response = api.createInventoryItem(request)
        val item = response.data?.toDomain() ?: return Result.Error("Server did not return the created item")
        Result.Success(item)
    } catch (e: ResponseException) {
        val errorBody = runCatching { e.response.bodyAsText() }.getOrNull()
        Result.Error(parseHttpError(e.response.status.value, errorBody))
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to add drug to inventory")
    }

    override suspend fun updateInventoryItem(
        id: String,
        drugNameRaw: String,
        quantityAvailable: Int,
        unitPrice: Double,
        discountPct: Double
    ): Result<InventoryItem> = try {
        val request = InventoryItemRequest(
            drugNameRaw = drugNameRaw,
            quantityAvailable = quantityAvailable,
            unitPrice = unitPrice,
            publicPrice = unitPrice,
            pharmacistPrice = unitPrice,
            discountPct = discountPct
        )
        val response = api.updateInventoryItem(id, request)
        val item = response.data?.toDomain() ?: return Result.Error("Server did not return the updated item")
        Result.Success(item)
    } catch (e: ResponseException) {
        val errorBody = runCatching { e.response.bodyAsText() }.getOrNull()
        Result.Error(parseHttpError(e.response.status.value, errorBody))
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update inventory item")
    }

    companion object {
        private const val TAG = "DrugRepository"
    }
}
