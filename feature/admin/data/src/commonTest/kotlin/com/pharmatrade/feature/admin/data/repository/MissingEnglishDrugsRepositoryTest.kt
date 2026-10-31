package com.pharmatrade.feature.admin.data.repository

import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.data.remote.AdminApi
import com.pharmatrade.feature.admin.domain.model.AdminForbiddenException
import com.pharmatrade.feature.admin.domain.model.DrugEnglishNameUpdate
import com.pharmatrade.feature.admin.domain.model.DrugField
import com.pharmatrade.feature.admin.domain.model.DrugFieldError
import com.pharmatrade.feature.admin.domain.model.DrugNotFoundException
import com.pharmatrade.feature.admin.domain.model.DrugValidationException
import com.pharmatrade.feature.admin.domain.model.MissingEnglishDrug
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MissingEnglishDrugsRepositoryTest {

    private val requests = mutableListOf<HttpRequestData>()

    // Same JSON settings and expectSuccess as ApiClient, so error statuses throw like in the app.
    private fun repository(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): AdminRepositoryImpl {
        val client = HttpClient(MockEngine { request -> requests += request; handler(request) }) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = true; isLenient = true })
            }
            defaultRequest { url("https://test.local/api/v1/") }
        }
        return AdminRepositoryImpl(AdminApi(client))
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    // ── GET admin/drugs/missing-english ────────────────────────────────────

    @Test
    fun listParsesTheDocumentedResponse() = runTest {
        val repo = repository {
            json(
                """
                {"message":"Drugs missing English names.","data":{"current_page":1,"data":[
                  {"id":25080,"commercial_name_ar":"كونكور 7.5","created_at":"2026-10-27T14:30:00.000000Z"},
                  {"id":25081,"commercial_name_ar":"دواء تجريبي 10","created_at":"2026-10-27T14:30:00.000000Z"}
                ],"per_page":20,"total":2,"last_page":1,"next_page_url":null,"prev_page_url":null}}
                """
            )
        }
        val page = (repo.getDrugsMissingEnglish(page = 1) as Result.Success).data

        assertEquals(
            listOf(
                MissingEnglishDrug("25080", "كونكور 7.5", "2026-10-27T14:30:00.000000Z"),
                MissingEnglishDrug("25081", "دواء تجريبي 10", "2026-10-27T14:30:00.000000Z")
            ),
            page.items
        )
        assertEquals(2, page.total)
        assertEquals(false, page.hasNextPage)

        val url = requests.single().url
        assertEquals("/api/v1/admin/drugs/missing-english", url.encodedPath)
        assertEquals("1", url.parameters["page"])
        assertEquals("20", url.parameters["per_page"])
    }

    @Test
    fun listReportsNextPageUntilTheLastPage() = runTest {
        val repo = repository {
            json("""{"data":{"current_page":1,"data":[],"total":45,"last_page":3}}""")
        }
        val page = (repo.getDrugsMissingEnglish(1) as Result.Success).data
        assertTrue(page.hasNextPage)
        assertEquals(45, page.total)
    }

    // ── PUT admin/drugs/{id} ──────────────────────────────────────────────

    @Test
    fun saveSendsNameAndTradeNameAndOnlyFilledOptionalFields() = runTest {
        val repo = repository { json("""{"message":"Drug updated successfully.","data":{"id":25080}}""") }

        val result = repo.saveDrugEnglishName(
            "25080",
            DrugEnglishNameUpdate(englishName = "CONCOR 7.5 MG 30 F.C. TABS.", strength = "7.5 mg")
        )

        assertIs<Result.Success<Unit>>(result)
        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/v1/admin/drugs/25080", request.url.encodedPath)
        assertEquals(
            Json.parseToJsonElement(
                """{"name":"CONCOR 7.5 MG 30 F.C. TABS.","trade_name":"CONCOR 7.5 MG 30 F.C. TABS.","strength":"7.5 mg"}"""
            ),
            Json.parseToJsonElement((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString())
        )
        assertTrue("application/json" in request.body.contentType.toString())
    }

    @Test
    fun saveMapsTakenBarcodeOn422ToTheBarcodeField() = runTest {
        val repo = repository {
            json(
                """{"message":"The barcode has already been taken.","errors":{"barcode":["The barcode has already been taken."]}}""",
                HttpStatusCode.UnprocessableEntity
            )
        }
        val result = assertIs<Result.Error>(repo.saveDrugEnglishName("1", DrugEnglishNameUpdate("X", barcode = "6221234567890")))
        assertEquals(
            mapOf(DrugField.BARCODE to DrugFieldError.ALREADY_TAKEN),
            assertIs<DrugValidationException>(result.exception).fieldErrors
        )
    }

    @Test
    fun saveMapsNameAndTradeNameErrorsToTheOneEnglishNameField() = runTest {
        val repo = repository {
            json(
                """{"message":"The name may not be greater than 255 characters.","errors":{
                    "name":["The name may not be greater than 255 characters."],
                    "trade_name":["The trade name may not be greater than 255 characters."]}}""",
                HttpStatusCode.UnprocessableEntity
            )
        }
        val result = assertIs<Result.Error>(repo.saveDrugEnglishName("1", DrugEnglishNameUpdate("X")))
        assertEquals(
            mapOf(DrugField.ENGLISH_NAME to DrugFieldError.TOO_LONG),
            assertIs<DrugValidationException>(result.exception).fieldErrors
        )
    }

    @Test
    fun saveMaps404ToDrugNotFound() = runTest {
        val repo = repository { json("""{"message":"Drug not found."}""", HttpStatusCode.NotFound) }
        val result = assertIs<Result.Error>(repo.saveDrugEnglishName("1", DrugEnglishNameUpdate("X")))
        assertIs<DrugNotFoundException>(result.exception)
        assertEquals("Drug not found.", result.message)
    }

    @Test
    fun listMaps403ToForbidden() = runTest {
        val repo = repository { json("""{"message":"Forbidden."}""", HttpStatusCode.Forbidden) }
        val result = assertIs<Result.Error>(repo.getDrugsMissingEnglish(1))
        assertIs<AdminForbiddenException>(result.exception)
    }

    @Test
    fun serverErrorIsAPlainError() = runTest {
        val repo = repository { json("""{"message":"Server Error"}""", HttpStatusCode.InternalServerError) }
        val result = assertIs<Result.Error>(repo.saveDrugEnglishName("1", DrugEnglishNameUpdate("X")))
        assertTrue(result.exception !is DrugValidationException && result.exception !is DrugNotFoundException)
    }
}
