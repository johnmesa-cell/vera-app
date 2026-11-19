package com.example.vera_app.domain.usecase

import com.example.vera_app.domain.model.AccountReference
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.InsufficientBalanceException
import com.example.vera_app.domain.model.Member
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.usecase.expense.RecordPersonalExpenseUseCase
import com.example.vera_app.domain.usecase.expense.RecordSharedExpenseUseCase
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.income.RecordPersonalIncomeUseCase
import com.example.vera_app.domain.usecase.income.RecordSharedIncomeUseCase
import com.example.vera_app.domain.usecase.member.AddMemberUseCase
import com.example.vera_app.domain.usecase.savings.CreateSavingsGoalUseCase
import com.example.vera_app.domain.usecase.savings.DepositToSavingsGoalUseCase
import com.example.vera_app.repository.FakeFamilyRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExpenseUseCasesTest {

    private lateinit var repository: FakeFamilyRepository
    private lateinit var createFamilyUseCase: CreateFamilyUseCase
    private lateinit var addMemberUseCase: AddMemberUseCase
    private lateinit var recordSharedIncomeUseCase: RecordSharedIncomeUseCase
    private lateinit var recordPersonalIncomeUseCase: RecordPersonalIncomeUseCase
    private lateinit var recordSharedExpenseUseCase: RecordSharedExpenseUseCase
    private lateinit var recordPersonalExpenseUseCase: RecordPersonalExpenseUseCase
    private lateinit var createSavingsGoalUseCase: CreateSavingsGoalUseCase
    private lateinit var depositToSavingsGoalUseCase: DepositToSavingsGoalUseCase

    @BeforeTest
    fun setUp() {
        repository = FakeFamilyRepository()
        createFamilyUseCase = CreateFamilyUseCase(repository)
        addMemberUseCase = AddMemberUseCase(repository)
        recordSharedIncomeUseCase = RecordSharedIncomeUseCase(repository)
        recordPersonalIncomeUseCase = RecordPersonalIncomeUseCase(repository)
        recordSharedExpenseUseCase = RecordSharedExpenseUseCase(repository)
        recordPersonalExpenseUseCase = RecordPersonalExpenseUseCase(repository)
        createSavingsGoalUseCase = CreateSavingsGoalUseCase(repository)
        depositToSavingsGoalUseCase = DepositToSavingsGoalUseCase(repository)
    }

    @Test
    fun testRecordSharedExpenseSuccessfully() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            recordSharedIncomeUseCase("fam_1", Money.fromMajorUnits(1_000_000L), "Ingreso", movementId = "mov_inc")

            val updatedFamily = recordSharedExpenseUseCase(
                familyId = "fam_1",
                amount = Money.fromMajorUnits(300_000L),
                description = "Mercado",
                movementId = "mov_exp_1",
            )

            assertEquals(Money.fromMajorUnits(700_000L), updatedFamily.sharedAccount.totalMoney)
            assertEquals(Money.fromMajorUnits(700_000L), updatedFamily.sharedAccount.availableMoney)
        }
    }

    @Test
    fun testRecordPersonalExpenseSuccessfully() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            addMemberUseCase("fam_1", Member("mem_1", "Carlos"))
            recordPersonalIncomeUseCase("fam_1", "mem_1", Money.fromMajorUnits(500_000L), "Ingreso Carlos", movementId = "mov_pinc")

            val updatedFamily = recordPersonalExpenseUseCase(
                familyId = "fam_1",
                memberId = "mem_1",
                amount = Money.fromMajorUnits(100_000L),
                description = "Libro",
                movementId = "mov_pexp_1",
            )

            val carlos = updatedFamily.getMember("mem_1")!!
            assertEquals(Money.fromMajorUnits(400_000L), carlos.personalAccount.totalMoney)
            assertEquals(Money.fromMajorUnits(400_000L), carlos.personalAccount.availableMoney)
        }
    }

    @Test
    fun testRejectsExpenseExceedingTotalBalance() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            recordSharedIncomeUseCase("fam_1", Money.fromMajorUnits(100_000L), "Ingreso", movementId = "mov_inc")

            val initialSaveCount = repository.saveFamilyCallCount

            assertFailsWith<InsufficientBalanceException> {
                recordSharedExpenseUseCase(
                    familyId = "fam_1",
                    amount = Money.fromMajorUnits(200_000L),
                    description = "Gasto Excesivo",
                    movementId = "mov_exp_fail",
                )
            }

            assertEquals(initialSaveCount, repository.saveFamilyCallCount)
            assertEquals(Money.fromMajorUnits(100_000L), repository.getFamilyById("fam_1")?.sharedAccount?.totalMoney)
        }
    }

    @Test
    fun testRejectsExpenseUsingReservedSavings() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            recordSharedIncomeUseCase("fam_1", Money.fromMajorUnits(1_000_000L), "Ingreso", movementId = "mov_inc")
            createSavingsGoalUseCase("fam_1", "goal_1", "Viaje", Money.fromMajorUnits(1_000_000L), AccountReference.SHARED)
            depositToSavingsGoalUseCase("fam_1", "goal_1", Money.fromMajorUnits(800_000L), movementId = "mov_dep")

            val familyState = repository.getFamilyById("fam_1")!!
            assertEquals(Money.fromMajorUnits(200_000L), familyState.sharedAccount.availableMoney)

            val initialSaveCount = repository.saveFamilyCallCount

            assertFailsWith<InsufficientBalanceException> {
                recordSharedExpenseUseCase(
                    familyId = "fam_1",
                    amount = Money.fromMajorUnits(300_000L),
                    description = "Gasto que toca ahorros",
                    movementId = "mov_exp_reserved",
                )
            }

            assertEquals(initialSaveCount, repository.saveFamilyCallCount)
        }
    }

    @Test
    fun testRecordExpenseThrowsWhenFamilyNotFound() {
        runBlocking {
            assertFailsWith<FamilyNotFoundException> {
                recordSharedExpenseUseCase(
                    familyId = "fam_ghost",
                    amount = Money.fromMajorUnits(100_000L),
                    description = "Gasto sin familia",
                    movementId = "mov_fail",
                )
            }
        }
    }
}
