package com.example.vera_app.domain.usecase

import com.example.vera_app.domain.model.DomainException
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.InvalidAmountException
import com.example.vera_app.domain.model.Member
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.income.RecordPersonalIncomeUseCase
import com.example.vera_app.domain.usecase.income.RecordSharedIncomeUseCase
import com.example.vera_app.domain.usecase.member.AddMemberUseCase
import com.example.vera_app.repository.FakeFamilyRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IncomeUseCasesTest {

    private lateinit var repository: FakeFamilyRepository
    private lateinit var createFamilyUseCase: CreateFamilyUseCase
    private lateinit var addMemberUseCase: AddMemberUseCase
    private lateinit var recordSharedIncomeUseCase: RecordSharedIncomeUseCase
    private lateinit var recordPersonalIncomeUseCase: RecordPersonalIncomeUseCase

    @BeforeTest
    fun setUp() {
        repository = FakeFamilyRepository()
        createFamilyUseCase = CreateFamilyUseCase(repository)
        addMemberUseCase = AddMemberUseCase(repository)
        recordSharedIncomeUseCase = RecordSharedIncomeUseCase(repository)
        recordPersonalIncomeUseCase = RecordPersonalIncomeUseCase(repository)
    }

    @Test
    fun testRecordSharedIncomeSuccessfully() = runTest {
        createFamilyUseCase("fam_1", "Familia Pérez")

        val updatedFamily = recordSharedIncomeUseCase(
            familyId = "fam_1",
            amount = Money.fromMajorUnits(1_000_000L),
            description = "Salario principal",
            movementId = "mov_inc_1",
        )

        assertEquals(Money.fromMajorUnits(1_000_000L), updatedFamily.sharedAccount.totalMoney)
        assertEquals(Money.fromMajorUnits(1_000_000L), updatedFamily.sharedAccount.availableMoney)
        assertEquals(1, updatedFamily.movements.size)
        assertEquals(1, repository.saveFamilyCallCount)
    }

    @Test
    fun testRecordPersonalIncomeSuccessfullyDoesNotAffectSharedAccount() = runTest {
        createFamilyUseCase("fam_1", "Familia Pérez")
        addMemberUseCase("fam_1", Member("mem_1", "Ana"))

        val updatedFamily = recordPersonalIncomeUseCase(
            familyId = "fam_1",
            memberId = "mem_1",
            amount = Money.fromMajorUnits(500_000L),
            description = "Trabajo Freelance",
            movementId = "mov_pinc_1",
        )

        val ana = updatedFamily.getMember("mem_1")!!
        assertEquals(Money.fromMajorUnits(500_000L), ana.personalAccount.totalMoney)
        assertEquals(Money.fromMajorUnits(500_000L), ana.personalAccount.availableMoney)

        assertEquals(Money.ZERO, updatedFamily.sharedAccount.totalMoney)
        assertEquals(Money.fromMajorUnits(500_000L), updatedFamily.totalFamilyMoney)
    }

    @Test
    fun testRecordSharedIncomeRejectsZeroOrNegativeAmount() = runTest {
        createFamilyUseCase("fam_1", "Familia Pérez")

        assertFailsWith<InvalidAmountException> {
            recordSharedIncomeUseCase(
                familyId = "fam_1",
                amount = Money.ZERO,
                description = "Monto nulo",
                movementId = "mov_zero",
            )
        }

        assertEquals(0, repository.saveFamilyCallCount)
    }

    @Test
    fun testRecordPersonalIncomeRejectsNonExistentMember() = runTest {
        createFamilyUseCase("fam_1", "Familia Pérez")

        assertFailsWith<DomainException> {
            recordPersonalIncomeUseCase(
                familyId = "fam_1",
                memberId = "mem_ghost",
                amount = Money.fromMajorUnits(100_000L),
                description = "Ingreso fantasma",
                movementId = "mov_ghost",
            )
        }

        assertEquals(0, repository.saveFamilyCallCount)
    }

    @Test
    fun testRecordSharedIncomeThrowsWhenFamilyNotFound() = runTest {
        assertFailsWith<FamilyNotFoundException> {
            recordSharedIncomeUseCase(
                familyId = "fam_ghost",
                amount = Money.fromMajorUnits(100_000L),
                description = "Sin familia",
                movementId = "mov_1",
            )
        }

        assertEquals(0, repository.saveFamilyCallCount)
    }
}
