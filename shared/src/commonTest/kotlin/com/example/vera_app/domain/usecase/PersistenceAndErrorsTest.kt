package com.example.vera_app.domain.usecase

import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.InsufficientBalanceException
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.usecase.expense.RecordSharedExpenseUseCase
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.income.RecordSharedIncomeUseCase
import com.example.vera_app.repository.FakeFamilyRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PersistenceAndErrorsTest {

    private lateinit var repository: FakeFamilyRepository
    private lateinit var createFamilyUseCase: CreateFamilyUseCase
    private lateinit var recordSharedIncomeUseCase: RecordSharedIncomeUseCase
    private lateinit var recordSharedExpenseUseCase: RecordSharedExpenseUseCase

    @BeforeTest
    fun setUp() {
        repository = FakeFamilyRepository()
        createFamilyUseCase = CreateFamilyUseCase(repository)
        recordSharedIncomeUseCase = RecordSharedIncomeUseCase(repository)
        recordSharedExpenseUseCase = RecordSharedExpenseUseCase(repository)
    }

    @Test
    fun testSuccessfulOperationPersistsUpdatedFamily() = runTest {
        createFamilyUseCase("fam_pers", "Familia Persistencia")
        assertEquals(1, repository.createFamilyCallCount)

        recordSharedIncomeUseCase(
            familyId = "fam_pers",
            amount = Money.fromMajorUnits(500_000L),
            description = "Ingreso",
            movementId = "mov_p1",
        )

        assertEquals(1, repository.saveFamilyCallCount)

        val persistedFamily = repository.getFamilyById("fam_pers")
        assertNotNull(persistedFamily)
        assertEquals(Money.fromMajorUnits(500_000L), persistedFamily.sharedAccount.totalMoney)
        assertEquals(1, persistedFamily.movements.size)
    }

    @Test
    fun testFailedOperationDoesNotPersistChanges() = runTest {
        createFamilyUseCase("fam_pers", "Familia Persistencia")
        recordSharedIncomeUseCase(
            familyId = "fam_pers",
            amount = Money.fromMajorUnits(100_000L),
            description = "Ingreso Base",
            movementId = "mov_p1",
        )

        val saveCountBefore = repository.saveFamilyCallCount
        val familyBefore = repository.getFamilyById("fam_pers")!!

        assertFailsWith<InsufficientBalanceException> {
            recordSharedExpenseUseCase(
                familyId = "fam_pers",
                amount = Money.fromMajorUnits(200_000L),
                description = "Gasto Excesivo",
                movementId = "mov_p2",
            )
        }

        assertEquals(saveCountBefore, repository.saveFamilyCallCount)

        val familyAfter = repository.getFamilyById("fam_pers")!!
        assertEquals(familyBefore.sharedAccount.totalMoney, familyAfter.sharedAccount.totalMoney)
        assertEquals(familyBefore.movements.size, familyAfter.movements.size)
    }

    @Test
    fun testBehaviorWhenRepositoryFamilyNotFound() = runTest {
        assertFailsWith<FamilyNotFoundException> {
            recordSharedIncomeUseCase(
                familyId = "fam_inexistente",
                amount = Money.fromMajorUnits(100_000L),
                description = "Prueba",
                movementId = "mov_p1",
            )
        }

        assertEquals(0, repository.saveFamilyCallCount)
    }
}
