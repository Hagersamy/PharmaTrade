package com.pharmatrade.feature.admin.presentation

import com.pharmatrade.core.common.i18n.EnglishStrings
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.domain.model.AdminForbiddenException
import com.pharmatrade.feature.admin.domain.model.DrugEnglishNameUpdate
import com.pharmatrade.feature.admin.domain.model.DrugField
import com.pharmatrade.feature.admin.domain.model.DrugFieldError
import com.pharmatrade.feature.admin.domain.model.DrugNotFoundException
import com.pharmatrade.feature.admin.domain.model.DrugValidationException
import com.pharmatrade.feature.admin.domain.model.MissingEnglishDrug
import com.pharmatrade.feature.admin.domain.model.MissingEnglishDrugsPage
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.model.RegistrationStats
import com.pharmatrade.feature.admin.domain.repository.AdminRepository
import com.pharmatrade.feature.admin.domain.usecase.GetDrugsMissingEnglishUseCase
import com.pharmatrade.feature.admin.domain.usecase.SaveDrugEnglishNameUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MissingEnglishDrugsViewModelTest {

    private val repository = FakeRepository()
    private lateinit var viewModel: MissingEnglishDrugsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = MissingEnglishDrugsViewModel(
            GetDrugsMissingEnglishUseCase(repository),
            SaveDrugEnglishNameUseCase(repository)
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun drug(id: Int) = MissingEnglishDrug(id.toString(), "دواء $id", "2026-10-27T14:30:00.000000Z")

    private fun page(n: Int, last: Int, total: Int, ids: IntRange) =
        MissingEnglishDrugsPage(ids.map(::drug), currentPage = n, lastPage = last, total = total)

    @Test
    fun loadsNothingUntilAsked() {
        assertTrue(repository.requestedPages.isEmpty())
        assertFalse(viewModel.uiState.value.loaded)
    }

    @Test
    fun refreshThenLoadMoreAppendsAndStopsOnTheLastPage() {
        repository.pages[1] = page(1, last = 2, total = 25, ids = 1..20)
        repository.pages[2] = page(2, last = 2, total = 25, ids = 21..25)

        viewModel.refresh()
        assertEquals(20, viewModel.uiState.value.drugs.size)
        assertEquals(25, viewModel.uiState.value.total)
        assertTrue(viewModel.uiState.value.hasNextPage)

        viewModel.loadMore()
        assertEquals((1..25).map { it.toString() }, viewModel.uiState.value.drugs.map { it.id })
        assertFalse(viewModel.uiState.value.hasNextPage)

        viewModel.loadMore()
        assertEquals(listOf(1, 2), repository.requestedPages)
    }

    @Test
    fun emptyListIsLoadedAndEmpty() {
        repository.pages[1] = page(1, last = 1, total = 0, ids = IntRange.EMPTY)
        viewModel.refresh()
        assertTrue(viewModel.uiState.value.loaded)
        assertTrue(viewModel.uiState.value.drugs.isEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun forbiddenListShowsTheAdminsOnlyMessage() {
        repository.listError = Result.Error("Forbidden.", AdminForbiddenException())
        viewModel.refresh()
        assertEquals(EnglishStrings.adAdminsOnly, viewModel.uiState.value.error)
    }

    @Test
    fun successfulSaveRemovesTheDrugLowersTotalAndClosesTheSheet() {
        repository.pages[1] = page(1, last = 1, total = 2, ids = 1..2)
        viewModel.refresh()

        viewModel.openEditor(drug(1))
        viewModel.onFormChange(DrugEnglishNameUpdate(englishName = "CONCOR 7.5 MG 30 F.C. TABS."))
        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(listOf("2"), state.drugs.map { it.id })
        assertEquals(1, state.total)
        assertNull(state.editing)
        assertEquals(EnglishStrings.adDrugNameSaved, state.toastMessage)
        assertEquals("1" to "CONCOR 7.5 MG 30 F.C. TABS.", repository.saved.single().let { it.first to it.second.englishName })
    }

    @Test
    fun blankNameKeepsTheSheetOpenWithAFieldErrorAndSendsNothing() {
        viewModel.openEditor(drug(1))
        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(drug(1), state.editing)
        assertEquals(mapOf(DrugField.ENGLISH_NAME to DrugFieldError.REQUIRED), state.fieldErrors)
        assertTrue(repository.saved.isEmpty())

        // Typing clears that input's error.
        viewModel.onFormChange(DrugEnglishNameUpdate(englishName = "C"))
        assertTrue(viewModel.uiState.value.fieldErrors.isEmpty())
    }

    @Test
    fun takenBarcodeOpensMoreDetailsAndFlagsTheBarcode() {
        repository.saveError = Result.Error(
            "The barcode has already been taken.",
            DrugValidationException(mapOf(DrugField.BARCODE to DrugFieldError.ALREADY_TAKEN))
        )
        viewModel.openEditor(drug(1))
        viewModel.onFormChange(DrugEnglishNameUpdate(englishName = "CONCOR", barcode = "622"))
        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(mapOf(DrugField.BARCODE to DrugFieldError.ALREADY_TAKEN), state.fieldErrors)
        assertTrue(state.showMoreDetails)
        assertEquals(drug(1), state.editing)
        assertFalse(state.isSaving)
    }

    @Test
    fun deletedDrugClosesTheSheetAndReloadsTheList() {
        repository.pages[1] = page(1, last = 1, total = 2, ids = 1..2)
        viewModel.refresh()
        repository.saveError = Result.Error("Drug not found.", DrugNotFoundException())
        repository.pages[1] = page(1, last = 1, total = 1, ids = 2..2)

        viewModel.openEditor(drug(1))
        viewModel.onFormChange(DrugEnglishNameUpdate(englishName = "CONCOR"))
        viewModel.save()

        val state = viewModel.uiState.value
        assertNull(state.editing)
        assertEquals(EnglishStrings.adDrugDeleted, state.toastMessage)
        assertEquals(listOf("2"), state.drugs.map { it.id })
        assertEquals(listOf(1, 1), repository.requestedPages)
    }

    @Test
    fun serverErrorKeepsTheSheetOpenWithARetryableMessage() {
        repository.saveError = Result.Error("Server error (500)")
        viewModel.openEditor(drug(1))
        viewModel.onFormChange(DrugEnglishNameUpdate(englishName = "CONCOR"))
        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(drug(1), state.editing)
        assertEquals(EnglishStrings.errorGeneric, state.saveError)
        assertFalse(state.isSaving)
    }

    private class FakeRepository : AdminRepository {
        val pages = mutableMapOf<Int, MissingEnglishDrugsPage>()
        val requestedPages = mutableListOf<Int>()
        var listError: Result.Error? = null
        var saveError: Result.Error? = null
        val saved = mutableListOf<Pair<String, DrugEnglishNameUpdate>>()

        override suspend fun getDrugsMissingEnglish(page: Int): Result<MissingEnglishDrugsPage> {
            requestedPages += page
            return listError ?: Result.Success(pages.getValue(page))
        }

        override suspend fun saveDrugEnglishName(id: String, update: DrugEnglishNameUpdate): Result<Unit> {
            saveError?.let { return it }
            saved += id to update
            return Result.Success(Unit)
        }

        override suspend fun getPendingPharmacies(): Result<List<PendingUser>> = error("unused")
        override suspend fun getPendingSuppliers(): Result<List<PendingUser>> = error("unused")
        override suspend fun approveUser(id: String, userType: UserType): Result<Unit> = error("unused")
        override suspend fun rejectUser(id: String, userType: UserType): Result<Unit> = error("unused")
        override suspend fun getRegistrationStats(): Result<RegistrationStats> = error("unused")
        override suspend fun getRegistrationRequests(entityType: String?): Result<List<PendingUser>> = error("unused")
        override suspend fun approveRequest(id: String, notes: String): Result<Unit> = error("unused")
        override suspend fun declineRequest(id: String, reason: String): Result<Unit> = error("unused")
    }
}
