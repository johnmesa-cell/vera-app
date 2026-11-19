package com.example.vera_app.domain.usecase

import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.InsufficientBalanceException
import com.example.vera_app.domain.model.Member
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.income.RecordPersonalIncomeUseCase
import com.example.vera_app.domain.usecase.income.RecordSharedIncomeUseCase
import com.example.vera_app.domain.usecase.member.AddMemberUseCase
import com.example.vera_app.domain.usecase.transfer.AllocateToMemberUseCase
import com.example.vera_app.domain.usecase.transfer.ReturnFromMemberUseCase
import com.example.vera_app.repository.FakeFamilyRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TransferUseCasesTest {

    private lateinit var repository: FakeFamilyRepository
    private lateinit var createFamilyUseCase: CreateFamilyUseCase
    private lateinit var addMemberUseCase: AddMemberUseCase
    private lateinit var recordSharedIncomeUseCase: RecordSharedIncomeUseCase
    private lateinit var recordPersonalIncomeUseCase: RecordPersonalIncomeUseCase
    private lateinit var allocateToMemberUseCase: AllocateToMemberUseCase
    private lateinit var returnFromMemberUseCase: ReturnFromMemberUseCase

    @BeforeTest
    fun setUp() {
        repository = FakeFamilyRepository()
        createFamilyUseCase = CreateFamilyUseCase(repository)
        addMemberUseCase = AddMemberUseCase(repository)
        recordSharedIncomeUseCase = RecordSharedIncomeUseCase(repository)
        recordPersonalIncomeUseCase = RecordPersonalIncomeUseCase(repository)
        allocateToMemberUseCase = AllocateToMemberUseCase(repository)
        returnFromMemberUseCase = ReturnFromMemberUseCase(repository)
    }

    @Test
    fun testAllocateToMemberSuccessfullyAndPreservesTotalFamilyMoney() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            addMemberUseCase("fam_1", Member("mem_1", "Carlos"))
            recordSharedIncomeUseCase("fam_1", Money.fromMajorUnits(1_000_000L), "Sueldo", movementId = "mov_inc")

            val totalBefore = repository.getFamilyById("fam_1")!!.totalFamilyMoney

            val updatedFamily = allocateToMemberUseCase(
                familyId = "fam_1",
                memberId = "mem_1",
                amount = Money.fromMajorUnits(300_000L),
                description = "Mesada Carlos",
                movementId = "mov_alloc_1",
            )

            val carlos = updatedFamily.getMember("mem_1")!!
            assertEquals(Money.fromMajorUnits(700_000L), updatedFamily.sharedAccount.totalMoney)
            assertEquals(Money.fromMajorUnits(300_000L), carlos.personalAccount.totalMoney)

            assertEquals(totalBefore, updatedFamily.totalFamilyMoney)
            assertEquals(Money.fromMajorUnits(1_000_000L), updatedFamily.totalFamilyMoney)
        }
    }

    @Test
    fun testReturnFromMemberSuccessfullyAndPreservesTotalFamilyMoney() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            addMemberUseCase("fam_1", Member("mem_1", "Ana"))
            recordPersonalIncomeUseCase("fam_1", "mem_1", Money.fromMajorUnits(500_000L), "Ingreso Ana", movementId = "mov_pinc")

            val totalBefore = repository.getFamilyById("fam_1")!!.totalFamilyMoney

            val updatedFamily = returnFromMemberUseCase(
                familyId = "fam_1",
                memberId = "mem_1",
                amount = Money.fromMajorUnits(200_000L),
                description = "Aporte Ana",
                movementId = "mov_ret_1",
            )

            val ana = updatedFamily.getMember("mem_1")!!
            assertEquals(Money.fromMajorUnits(300_000L), ana.personalAccount.totalMoney)
            assertEquals(Money.fromMajorUnits(200_000L), updatedFamily.sharedAccount.totalMoney)

            assertEquals(totalBefore, updatedFamily.totalFamilyMoney)
            assertEquals(Money.fromMajorUnits(500_000L), updatedFamily.totalFamilyMoney)
        }
    }

    @Test
    fun testAllocateToMemberRejectsExceedingSharedBalance() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            addMemberUseCase("fam_1", Member("mem_1", "Carlos"))
            recordSharedIncomeUseCase("fam_1", Money.fromMajorUnits(100_000L), "Sueldo", movementId = "mov_inc")

            val saveCountBefore = repository.saveFamilyCallCount

            assertFailsWith<InsufficientBalanceException> {
                allocateToMemberUseCase(
                    familyId = "fam_1",
                    memberId = "mem_1",
                    amount = Money.fromMajorUnits(200_000L),
                    description = "Mesada Excesiva",
                    movementId = "mov_alloc_fail",
                )
            }

            assertEquals(saveCountBefore, repository.saveFamilyCallCount)
        }
    }

    @Test
    fun testReturnFromMemberRejectsExceedingPersonalBalance() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            addMemberUseCase("fam_1", Member("mem_1", "Carlos"))
            recordPersonalIncomeUseCase("fam_1", "mem_1", Money.fromMajorUnits(100_000L), "Ingreso Carlos", movementId = "mov_pinc")

            val saveCountBefore = repository.saveFamilyCallCount

            assertFailsWith<InsufficientBalanceException> {
                returnFromMemberUseCase(
                    familyId = "fam_1",
                    memberId = "mem_1",
                    amount = Money.fromMajorUnits(200_000L),
                    description = "Devolución Excesiva",
                    movementId = "mov_ret_fail",
                )
            }

            assertEquals(saveCountBefore, repository.saveFamilyCallCount)
        }
    }

    @Test
    fun testTransferThrowsWhenFamilyNotFound() {
        runBlocking {
            assertFailsWith<FamilyNotFoundException> {
                allocateToMemberUseCase(
                    familyId = "fam_ghost",
                    memberId = "mem_1",
                    amount = Money.fromMajorUnits(100_000L),
                    description = "Sin familia",
                    movementId = "mov_alloc_ghost",
                )
            }
        }
    }
}
