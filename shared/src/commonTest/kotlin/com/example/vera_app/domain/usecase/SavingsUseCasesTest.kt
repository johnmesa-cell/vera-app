package com.example.vera_app.domain.usecase

import com.example.vera_app.domain.model.AccountReference
import com.example.vera_app.domain.model.DomainException
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.InsufficientBalanceException
import com.example.vera_app.domain.model.InsufficientSavingsException
import com.example.vera_app.domain.model.Member
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.income.RecordPersonalIncomeUseCase
import com.example.vera_app.domain.usecase.income.RecordSharedIncomeUseCase
import com.example.vera_app.domain.usecase.member.AddMemberUseCase
import com.example.vera_app.domain.usecase.savings.CreateSavingsGoalUseCase
import com.example.vera_app.domain.usecase.savings.DepositToSavingsGoalUseCase
import com.example.vera_app.domain.usecase.savings.WithdrawFromSavingsGoalUseCase
import com.example.vera_app.repository.FakeFamilyRepository
import kotlinx.coroutines.test.runTest
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
    private lateinit var recordPersonalIncomeUseCase: RecordPersonalIncomeUseCase
    private lateinit var createSavingsGoalUseCase: CreateSavingsGoalUseCase
    private lateinit var depositToSavingsGoalUseCase: DepositToSavingsGoalUseCase
    private lateinit var withdrawFromSavingsGoalUseCase: WithdrawFromSavingsGoalUseCase

    @BeforeTest
    fun setUp() {
        repository = FakeFamilyRepository()
        createFamilyUseCase = CreateFamilyUseCase(repository)
        addMemberUseCase = AddMemberUseCase(repository)
        recordSharedIncomeUseCase = RecordSharedIncomeUseCase(repository)
        recordPersonalIncomeUseCase = RecordPersonalIncomeUseCase(repository)
        createSavingsGoalUseCase = CreateSavingsGoalUseCase(repository)
        depositToSavingsGoalUseCase = DepositToSavingsGoalUseCase(repository)
        withdrawFromSavingsGoalUseCase = WithdrawFromSavingsGoalUseCase(repository)
    }

    @Test
    fun testCreateSavingsGoalSuccessfully() = runTest {
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

    @Test
    fun testCreateSavingsGoalRejectsDuplicateId() = runTest {
        createFamilyUseCase("fam_1", "Familia Pérez")
        createSavingsGoalUseCase("fam_1", "goal_1", "Meta 1", Money.fromMajorUnits(500_000L), AccountReference.SHARED)

        val saveCountBefore = repository.saveFamilyCallCount

        assertFailsWith<IllegalArgumentException> {
            createSavingsGoalUseCase("fam_1", "goal_1", "Meta Duplicada", Money.fromMajorUnits(500_000L), AccountReference.SHARED)
        }

        assertEquals(saveCountBefore, repository.saveFamilyCallCount)
    }

    @Test
    fun testDepositAndWithdrawKeepCoherenceWithReservedSavings() = runTest {
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

    @Test
    fun testDepositRejectsExceedingTargetAmount() = runTest {
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

    @Test
    fun testWithdrawRejectsExceedingCurrentSavings() = runTest {
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

    // --- PRUEBAS DE AHORRO PERSONAL ---

    @Test
    fun testCreatePersonalSavingsGoalAndCheckAssociation() = runTest {
        createFamilyUseCase("fam_1", "Familia Pérez")
        addMemberUseCase("fam_1", Member("mem_1", "Carlos"))

        val updatedFamily = createSavingsGoalUseCase(
            familyId = "fam_1",
            id = "goal_carlos_1",
            name = "Auto Carlos",
            targetAmount = Money.fromMajorUnits(10_000_000L),
            accountRef = AccountReference.personal("mem_1"),
        )

        val goal = updatedFamily.getSavingsGoal("goal_carlos_1")
        assertNotNull(goal)
        assertEquals("Auto Carlos", goal.name)
        assertEquals(AccountReference.personal("mem_1"), goal.accountRef)
        assertEquals("mem_1", goal.accountRef.memberId)
    }

    @Test
    fun testDepositAndWithdrawPersonalSavingsGoal() = runTest {
        createFamilyUseCase("fam_1", "Familia Pérez")
        addMemberUseCase("fam_1", Member("mem_1", "Carlos"))
        recordPersonalIncomeUseCase("fam_1", "mem_1", Money.fromMajorUnits(1_000_000L), "Ingreso Carlos", movementId = "mov_pinc")
        createSavingsGoalUseCase("fam_1", "goal_carlos_1", "Curso", Money.fromMajorUnits(500_000L), AccountReference.personal("mem_1"))

        val totalFamilyBefore = repository.getFamilyById("fam_1")!!.totalFamilyMoney

        // Depositar dinero en la meta personal
        var family = depositToSavingsGoalUseCase(
            familyId = "fam_1",
            savingsGoalId = "goal_carlos_1",
            amount = Money.fromMajorUnits(300_000L),
            movementId = "mov_dep_p1",
        )

        var carlos = family.getMember("mem_1")!!
        var goal = family.getSavingsGoal("goal_carlos_1")!!

        // Verificar incrementos de meta y reservas personales
        assertEquals(Money.fromMajorUnits(300_000L), goal.currentAmount)
        assertEquals(Money.fromMajorUnits(300_000L), carlos.personalAccount.reservedSavings)
        // Verificar disminución de disponible personal sin cambiar el total personal ni familiar
        assertEquals(Money.fromMajorUnits(700_000L), carlos.personalAccount.availableMoney)
        assertEquals(Money.fromMajorUnits(1_000_000L), carlos.personalAccount.totalMoney)
        assertEquals(totalFamilyBefore, family.totalFamilyMoney)

        // Retirar dinero de la meta personal
        family = withdrawFromSavingsGoalUseCase(
            familyId = "fam_1",
            savingsGoalId = "goal_carlos_1",
            amount = Money.fromMajorUnits(100_000L),
            movementId = "mov_with_p1",
        )

        carlos = family.getMember("mem_1")!!
        goal = family.getSavingsGoal("goal_carlos_1")!!

        // Verificar reducciones de meta y reservas, e incremento de disponible personal
        assertEquals(Money.fromMajorUnits(200_000L), goal.currentAmount)
        assertEquals(Money.fromMajorUnits(200_000L), carlos.personalAccount.reservedSavings)
        assertEquals(Money.fromMajorUnits(800_000L), carlos.personalAccount.availableMoney)
        assertEquals(Money.fromMajorUnits(1_000_000L), carlos.personalAccount.totalMoney)
        assertEquals(totalFamilyBefore, family.totalFamilyMoney)
    }

    @Test
    fun testPersonalSavingsRejectsDepositExceedingAvailablePersonalBalance() = runTest {
        createFamilyUseCase("fam_1", "Familia Pérez")
        addMemberUseCase("fam_1", Member("mem_1", "Carlos"))
        recordPersonalIncomeUseCase("fam_1", "mem_1", Money.fromMajorUnits(100_000L), "Ingreso Carlos", movementId = "mov_pinc")
        createSavingsGoalUseCase("fam_1", "goal_carlos_1", "Laptop", Money.fromMajorUnits(500_000L), AccountReference.personal("mem_1"))

        val saveCountBefore = repository.saveFamilyCallCount

        // Intentar depositar 200,000 teniendo sólo 100,000 disponibles
        assertFailsWith<InsufficientBalanceException> {
            depositToSavingsGoalUseCase(
                familyId = "fam_1",
                savingsGoalId = "goal_carlos_1",
                amount = Money.fromMajorUnits(200_000L),
                movementId = "mov_dep_p_excess",
            )
        }

        assertEquals(saveCountBefore, repository.saveFamilyCallCount)
    }

    @Test
    fun testPersonalSavingsRejectsWithdrawalExceedingAccumulatedGoalAmount() = runTest {
        createFamilyUseCase("fam_1", "Familia Pérez")
        addMemberUseCase("fam_1", Member("mem_1", "Carlos"))
        recordPersonalIncomeUseCase("fam_1", "mem_1", Money.fromMajorUnits(500_000L), "Ingreso Carlos", movementId = "mov_pinc")
        createSavingsGoalUseCase("fam_1", "goal_carlos_1", "Celular", Money.fromMajorUnits(500_000L), AccountReference.personal("mem_1"))
        depositToSavingsGoalUseCase("fam_1", "goal_carlos_1", Money.fromMajorUnits(100_000L), movementId = "mov_dep_p1")

        val saveCountBefore = repository.saveFamilyCallCount

        // Intentar retirar 150,000 teniendo sólo 100,000 acumulados en la meta
        assertFailsWith<InsufficientSavingsException> {
            withdrawFromSavingsGoalUseCase(
                familyId = "fam_1",
                savingsGoalId = "goal_carlos_1",
                amount = Money.fromMajorUnits(150_000L),
                movementId = "mov_with_p_excess",
            )
        }

        assertEquals(saveCountBefore, repository.saveFamilyCallCount)
    }

    @Test
    fun testSavingsUseCasesThrowWhenFamilyNotFound() = runTest {
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
