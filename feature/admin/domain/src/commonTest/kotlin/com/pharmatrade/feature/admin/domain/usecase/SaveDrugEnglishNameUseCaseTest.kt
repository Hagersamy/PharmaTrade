package com.pharmatrade.feature.admin.domain.usecase

import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.admin.domain.model.DrugEnglishNameUpdate
import com.pharmatrade.feature.admin.domain.model.DrugField
import com.pharmatrade.feature.admin.domain.model.DrugFieldError
import com.pharmatrade.feature.admin.domain.model.DrugValidationException
import com.pharmatrade.feature.admin.domain.model.MissingEnglishDrugsPage
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.admin.domain.model.RegistrationStats
import com.pharmatrade.feature.admin.domain.repository.AdminRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SaveDrugEnglishNameUseCaseTest {

    private val repository = RecordingRepository()
    private val useCase = SaveDrugEnglishNameUseCase(repository)

    private suspend fun fieldErrorsFor(update: DrugEnglishNameUpdate): Map<DrugField, DrugFieldError> {
        val result = useCase("1", update)
        assertIs<Result.Error>(result)
        return assertIs<DrugValidationException>(result.exception).fieldErrors
    }

    @Test
    fun blankNameIsRequiredAndNothingIsSent() = runTest {
        assertEquals(mapOf(DrugField.ENGLISH_NAME to DrugFieldError.REQUIRED), fieldErrorsFor(DrugEnglishNameUpdate("   ")))
        assertNull(repository.saved)
    }

    @Test
    fun arabicNameIsRejected() = runTest {
        assertEquals(
            mapOf(DrugField.ENGLISH_NAME to DrugFieldError.ENGLISH_ONLY),
            fieldErrorsFor(DrugEnglishNameUpdate("كونكور 7.5"))
        )
        assertNull(repository.saved)
    }

    @Test
    fun overLongFieldsAreRejected() = runTest {
        val errors = fieldErrorsFor(
            DrugEnglishNameUpdate(englishName = "A".repeat(256), strength = "1".repeat(101), barcode = "2".repeat(101))
        )
        assertEquals(
            mapOf(
                DrugField.ENGLISH_NAME to DrugFieldError.TOO_LONG,
                DrugField.STRENGTH to DrugFieldError.TOO_LONG,
                DrugField.BARCODE to DrugFieldError.TOO_LONG
            ),
            errors
        )
    }

    @Test
    fun validInputIsTrimmedAndSent() = runTest {
        val result = useCase("25080", DrugEnglishNameUpdate(englishName = "  CONCOR 7.5 MG 30 F.C. TABS. ", barcode = " 622 "))
        assertIs<Result.Success<Unit>>(result)
        assertEquals("25080", repository.savedId)
        assertEquals(DrugEnglishNameUpdate(englishName = "CONCOR 7.5 MG 30 F.C. TABS.", barcode = "622"), repository.saved)
    }

    private class RecordingRepository : AdminRepository {
        var savedId: String? = null
        var saved: DrugEnglishNameUpdate? = null

        override suspend fun saveDrugEnglishName(id: String, update: DrugEnglishNameUpdate): Result<Unit> {
            savedId = id
            saved = update
            return Result.Success(Unit)
        }

        override suspend fun getDrugsMissingEnglish(page: Int): Result<MissingEnglishDrugsPage> = error("unused")
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
