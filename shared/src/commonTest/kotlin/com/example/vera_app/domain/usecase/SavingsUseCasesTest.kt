package com.example.vera_app.domain.usecase

import com.example.vera_app.domain.model.AccountReference
import com.example.vera_app.domain.model.DomainException
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.InsufficientSavingsException
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.income.RecordSharedIncomeUseCase
import com.example.vera_app.domain.usecase.member.AddMemberUseCase
import com.example.vera_app.domain.usecase.savings.CreateSavingsGoalUseCase
import com.example.vera_app.domain.usecase.savings.DepositToSavingsGoalUseCase
import com.example.vera_app.domain.usecase.savings.WithdrawFromSavingsGoalUseCase
import com.example.vera_app.repository.FakeFamilyRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class SavingsUseCasesTest {

    private lateinit var repository: FakeFamilyRepository
    private lateinit var createFamilyUseCase: CreateFamilyUseCase
    private lateinit var addMemberUseCase: AddMemberUseCase
    private lateinit var recordSharedIncomeUseCase: RecordSharedIncomeUseCase
    private lateinit var createSavingsGoalUseCase: CreateSavingsGoalUseCase
    private lateinit var depositToSavingsGoalUseCase: DepositToSavingsGoalUseCase
    private lateinit var withdrawFromSavingsGoalUseCase: WithdrawFromSavingsGoalUseCase

    @BeforeTest
    fun setUp() {
        repository = FakeFamilyRepository()
        createFamilyUseCase = CreateFamilyUseCase(repository)
        addMemberUseCase = AddMemberUseCase(repository)
        recordSharedIncomeUseCase = RecordSharedIncomeUseCase(repository)
        createSavingsGoalUseCase = CreateSavingsGoalUseCase(repository)
        depositToSavingsGoalUseCase = DepositToSavingsGoalUseCase(repository)
        withdrawFromSavingsGoalUseCase = WithdrawFromSavingsGoalUseCase(repository)
    }

    @Test
    fun testCreateSavingsGoalSuccessfully() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")

            val updatedFamily = createSavingsGoalUseCase(
                familyId = "fam_1",
                id = "goal_1",
                name = "Fondo de Emergencia",
                targetAmount = Money.fromMajorUnits(1_000_000L),
                accountRef = AccountReference.SHARED,
            )

            val goal = updatedFamily.getSavingsGoal("goal_1")
            assertNotNull(goal)
            assertEquals("Fondo de Emergencia", goal.name)
            assertEquals(Money.ZERO, goal.currentAmount)
            assertEquals(Money.fromMajorUnits(1_000_000L), goal.targetAmount)
        }
    }

    @Test
    fun testCreateSavingsGoalRejectsDuplicateId() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            createSavingsGoalUseCase("fam_1", "goal_1", "Meta 1", Money.fromMajorUnits(500_000L), AccountReference.SHARED)

            val saveCountBefore = repository.saveFamilyCallCount

            assertFailsWith<IllegalArgumentException> {
                createSavingsGoalUseCase("fam_1", "goal_1", "Meta Duplicada", Money.fromMajorUnits(500_000L), AccountReference.SHARED)
            }

            assertEquals(saveCountBefore, repository.saveFamilyCallCount)
        }
    }

    @Test
    fun testDepositAndWithdrawKeepCoherenceWithReservedSavings() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            recordSharedIncomeUseCase("fam_1", Money.fromMajorUnits(1_000_000L), "Sueldo", movementId = "mov_inc")
            createSavingsGoalUseCase("fam_1", "goal_1", "Vacaciones", Money.fromMajorUnits(500_000L), AccountReference.SHARED)

            var family = depositToSavingsGoalUseCase(
                familyId = "fam_1",
                savingsGoalId = "goal_1",
                amount = Money.fromMajorUnits(300_000L),
                movementId = "mov_dep_1",
            )

            var goal = family.getSavingsGoal("goal_1")!!
            assertEquals(Money.fromMajorUnits(300_000L), goal.currentAmount)
            assertEquals(Money.fromMajorUnits(300_000L), family.sharedAccount.reservedSavings)
            assertEquals(Money.fromMajorUnits(700_000L), family.sharedAccount.availableMoney)
            assertEquals(Money.fromMajorUnits(1_000_000L), family.sharedAccount.totalMoney)

            family = withdrawFromSavingsGoalUseCase(
                familyId = "fam_1",
                savingsGoalId = "goal_1",
                amount = Money.fromMajorUnits(100_000L),
                movementId = "mov_with_1",
            )

            goal = family.getSavingsGoal("goal_1")!!
            assertEquals(Money.fromMajorUnits(200_000L), goal.currentAmount)
            assertEquals(Money.fromMajorUnits(200_000L), family.sharedAccount.reservedSavings)
            assertEquals(Money.fromMajorUnits(800_000L), family.sharedAccount.availableMoney)
            assertEquals(Money.fromMajorUnits(1_000_000L), family.sharedAccount.totalMoney)
        }
    }

    @Test
    fun testDepositRejectsExceedingTargetAmount() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            recordSharedIncomeUseCase("fam_1", Money.fromMajorUnits(1_000_000L), "Sueldo", movementId = "mov_inc")
            createSavingsGoalUseCase("fam_1", "goal_1", "Viaje Corto", Money.fromMajorUnits(200_000L), AccountReference.SHARED)

            val saveCountBefore = repository.saveFamilyCallCount

            assertFailsWith<DomainException> {
                depositToSavingsGoalUseCase(
                    familyId = "fam_1",
                    savingsGoalId = "goal_1",
                    amount = Money.fromMajorUnits(300_000L),
                    movementId = "mov_dep_excess",
                )
            }

            assertEquals(saveCountBefore, repository.saveFamilyCallCount)
        }
    }

    @Test
    fun testWithdrawRejectsExceedingCurrentSavings() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")
            recordSharedIncomeUseCase("fam_1", Money.fromMajorUnits(1_000_000L), "Sueldo", movementId = "mov_inc")
            createSavingsGoalUseCase("fam_1", "goal_1", "Fondo", Money.fromMajorUnits(500_000L), AccountReference.SHARED)
            depositToSavingsGoalUseCase("fam_1", "goal_1", Money.fromMajorUnits(100_000L), movementId = "mov_dep_1")

            val saveCountBefore = repository.saveFamilyCallCount

            assertFailsWith<InsufficientSavingsException> {
                withdrawFromSavingsGoalUseCase(
                    familyId = "fam_1",
                    savingsGoalId = "goal_1",
                    amount = Money.fromMajorUnits(200_000L),
                    movementId = "mov_with_excess",
                )
            }

            assertEquals(saveCountBefore, repository.saveFamilyCallCount)
        }
    }

    @Test
    fun testSavingsUseCasesThrowWhenFamilyNotFound() {
        runBlocking {
            assertFailsWith<FamilyNotFoundException> {
                createSavingsGoalUseCase("fam_ghost", "goal_1", "Meta", Money.fromMajorUnits(100_000L), AccountReference.SHARED)
            }

            assertFailsWith<FamilyNotFoundException> {
                depositToSavingsGoalUseCase("fam_ghost", "goal_1", Money.fromMajorUnits(50_000L), movementId = "mov_1")
            }

            assertFailsWith<FamilyNotFoundException> {
                withdrawFromSavingsGoalUseCase("fam_ghost", "goal_1", Money.fromMajorUnits(50_000L), movementId = "mov_2")
            }
        }
    }
}
