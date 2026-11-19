package com.example.vera_app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DomainModelTest {

    // --- 1. Pruebas de Money y desbordamientos ---

    @Test
    fun testMoneyNormalOperationsAndUnits() {
        val hundredPesos = Money.fromMajorUnits(100L)
        assertEquals(10_000L, hundredPesos.toCents())
        assertEquals(100L, hundredPesos.toMajorUnits())

        val fiftyCents = Money.of(50L)
        assertEquals(50L, fiftyCents.toCents())
        assertEquals(0L, fiftyCents.toMajorUnits())

        val m1 = Money.fromMajorUnits(100L)
        val m2 = Money.fromMajorUnits(50L)
        assertEquals(Money.fromMajorUnits(150L), m1 + m2)
        assertEquals(Money.fromMajorUnits(50L), m1 - m2)
        assertTrue(m1 > m2)
    }

    @Test
    fun testMoneyOverflowAndNegativeRejection() {
        assertFailsWith<ArithmeticException> {
            Money(Long.MAX_VALUE) + Money(1L)
        }

        assertFailsWith<ArithmeticException> {
            Money.fromMajorUnits(Long.MAX_VALUE)
        }

        assertFailsWith<IllegalArgumentException> {
            Money.of(-100L)
        }
        assertFailsWith<IllegalArgumentException> {
            Money.fromMajorUnits(-5L)
        }
    }

    @Test
    fun testRejectionOfInvalidMovementAmounts() {
        assertFailsWith<InvalidAmountException> {
            Movement(
                id = "mov_invalid",
                type = MovementType.INCOME,
                amount = Money.ZERO,
                description = "Monto Cero",
                categoryId = Category.SYSTEM_INCOME.id,
                destinationAccountRef = AccountReference.SHARED,
            )
        }
    }

    // --- 2. Pruebas de IDs duplicados ---

    @Test
    fun testRejectionOfDuplicateMemberIds() {
        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                members = listOf(
                    Member(id = "mem_1", name = "Carlos"),
                    Member(id = "mem_1", name = "Duplicado"),
                ),
            )
        }
    }

    @Test
    fun testRejectionOfDuplicateSavingsGoalIds() {
        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                savingsGoals = listOf(
                    SavingsGoal(id = "goal_1", name = "Meta A", targetAmount = Money(1000L), accountRef = AccountReference.SHARED),
                    SavingsGoal(id = "goal_1", name = "Meta B", targetAmount = Money(2000L), accountRef = AccountReference.SHARED),
                ),
            )
        }
    }

    @Test
    fun testRejectionOfDuplicateMovementIdsOnFamilyConstruction() {
        val m1 = Movement(
            id = "mov_dup",
            type = MovementType.INCOME,
            amount = Money(100L),
            description = "Ingreso 1",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        val m2 = Movement(
            id = "mov_dup",
            type = MovementType.INCOME,
            amount = Money(200L),
            description = "Ingreso 2",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )

        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                movements = listOf(m1, m2),
            )
        }
    }

    // --- 3. Pruebas de referencias incorrectas en metas ---

    @Test
    fun testRejectionOfSavingsGoalLinkedToNonExistentMember() {
        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                savingsGoals = listOf(
                    SavingsGoal(
                        id = "goal_ghost",
                        name = "Meta Fantasma",
                        targetAmount = Money(1000L),
                        accountRef = AccountReference.personal("non_existent_member"),
                    ),
                ),
            )
        }
    }

    // --- 4. Pruebas de reservas inconsistentes por cuenta ---

    @Test
    fun testRejectionOfInconsistentSharedAccountReservations() {
        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                sharedAccount = SharedAccount(totalMoney = Money(1000L), reservedSavings = Money(500L)),
                savingsGoals = emptyList(),
            )
        }
    }

    @Test
    fun testRejectionOfInconsistentPersonalAccountReservations() {
        val member = Member(
            id = "mem_1",
            name = "Ana",
            personalAccount = PersonalAccount(memberId = "mem_1", totalMoney = Money(1000L), reservedSavings = Money(300L)),
        )
        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                members = listOf(member),
                savingsGoals = emptyList(),
            )
        }
    }

    @Test
    fun testValidFamilyConstructionWithMultipleConsistentGoals() {
        val member = Member(
            id = "mem_1",
            name = "Ana",
            personalAccount = PersonalAccount(memberId = "mem_1", totalMoney = Money(1000L), reservedSavings = Money(100L)),
        )
        val sharedAccount = SharedAccount(totalMoney = Money(2000L), reservedSavings = Money(200L))

        val sharedGoal = SavingsGoal(
            id = "goal_sh",
            name = "Meta Familiar",
            targetAmount = Money(500L),
            currentAmount = Money(200L),
            accountRef = AccountReference.SHARED,
        )
        val personalGoal = SavingsGoal(
            id = "goal_pers",
            name = "Meta Personal",
            targetAmount = Money(300L),
            currentAmount = Money(100L),
            accountRef = AccountReference.personal("mem_1"),
        )

        val family = Family(
            id = "fam_1",
            name = "Familia Pérez",
            sharedAccount = sharedAccount,
            members = listOf(member),
            savingsGoals = listOf(sharedGoal, personalGoal),
        )

        assertEquals(Money(200L), family.sharedAccount.reservedSavings)
        assertEquals(Money(100L), family.getMember("mem_1")!!.personalAccount.reservedSavings)
        assertEquals(Money(300L), family.totalFamilyReservedSavings)
    }

    // --- 5. Pruebas de integridad de movimientos y referencias ---

    @Test
    fun testRejectionOfIncomeReferencingNonExistentMemberAccount() {
        val invalidIncome = Movement(
            id = "mov_inc_bad",
            type = MovementType.INCOME,
            amount = Money(1000L),
            description = "Ingreso Invalido",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.personal("ghost_member"),
        )
        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                movements = listOf(invalidIncome),
            )
        }
    }

    @Test
    fun testRejectionOfExpenseReferencingNonExistentMemberAccount() {
        val invalidExpense = Movement(
            id = "mov_exp_bad",
            type = MovementType.EXPENSE,
            amount = Money(500L),
            description = "Gasto Invalido",
            categoryId = Category.SYSTEM_EXPENSE.id,
            sourceAccountRef = AccountReference.personal("ghost_member"),
        )
        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                movements = listOf(invalidExpense),
            )
        }
    }

    @Test
    fun testRejectionOfTransferReferencingNonExistentMemberAccount() {
        val invalidTransfer = Movement(
            id = "mov_tr_bad",
            type = MovementType.TRANSFER,
            amount = Money(500L),
            description = "Transferencia Invalida",
            categoryId = Category.SYSTEM_ALLOCATION.id,
            sourceAccountRef = AccountReference.SHARED,
            destinationAccountRef = AccountReference.personal("ghost_member"),
        )
        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                movements = listOf(invalidTransfer),
            )
        }
    }

    @Test
    fun testRejectionOfSavingsDepositWithNonExistentGoalId() {
        val invalidDeposit = Movement(
            id = "mov_dep_bad",
            type = MovementType.SAVINGS_DEPOSIT,
            amount = Money(200L),
            description = "Reserva Ahorro Invalida",
            categoryId = Category.SYSTEM_SAVINGS.id,
            sourceAccountRef = AccountReference.SHARED,
            savingsGoalId = "non_existent_goal",
        )
        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                movements = listOf(invalidDeposit),
            )
        }
    }

    @Test
    fun testRejectionOfSavingsWithdrawalWithNonExistentGoalId() {
        val invalidWithdrawal = Movement(
            id = "mov_with_bad",
            type = MovementType.SAVINGS_WITHDRAWAL,
            amount = Money(200L),
            description = "Retiro Ahorro Invalido",
            categoryId = Category.SYSTEM_SAVINGS.id,
            destinationAccountRef = AccountReference.SHARED,
            savingsGoalId = "non_existent_goal",
        )
        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                movements = listOf(invalidWithdrawal),
            )
        }
    }

    @Test
    fun testRejectionOfSavingsDepositWithMismatchedSourceAccount() {
        val goal = SavingsGoal(
            id = "goal_shared",
            name = "Meta Compartida",
            targetAmount = Money(1000L),
            accountRef = AccountReference.SHARED,
        )
        val member = Member(id = "mem_1", name = "Carlos")

        // El depósito intenta usar la cuenta personal de Carlos como origen, pero la meta es compartida
        val mismatchedDeposit = Movement(
            id = "mov_dep_mismatch",
            type = MovementType.SAVINGS_DEPOSIT,
            amount = Money(200L),
            description = "Depósito Mismatched",
            categoryId = Category.SYSTEM_SAVINGS.id,
            sourceAccountRef = AccountReference.personal("mem_1"),
            savingsGoalId = "goal_shared",
        )

        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                members = listOf(member),
                savingsGoals = listOf(goal),
                movements = listOf(mismatchedDeposit),
            )
        }
    }

    @Test
    fun testRejectionOfSavingsWithdrawalWithMismatchedDestinationAccount() {
        val goal = SavingsGoal(
            id = "goal_shared",
            name = "Meta Compartida",
            targetAmount = Money(1000L),
            accountRef = AccountReference.SHARED,
        )
        val member = Member(id = "mem_1", name = "Carlos")

        // El retiro intenta enviar fondos a la cuenta personal de Carlos, pero la meta es compartida
        val mismatchedWithdrawal = Movement(
            id = "mov_with_mismatch",
            type = MovementType.SAVINGS_WITHDRAWAL,
            amount = Money(200L),
            description = "Retiro Mismatched",
            categoryId = Category.SYSTEM_SAVINGS.id,
            destinationAccountRef = AccountReference.personal("mem_1"),
            savingsGoalId = "goal_shared",
        )

        assertFailsWith<IllegalArgumentException> {
            Family(
                id = "fam_1",
                name = "Familia Pérez",
                members = listOf(member),
                savingsGoals = listOf(goal),
                movements = listOf(mismatchedWithdrawal),
            )
        }
    }

    @Test
    fun testValidFamilyConstructionWithValidMovements() {
        val member = Member(id = "mem_1", name = "Carlos")
        val income = Movement(
            id = "mov_inc_1",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(1000L),
            description = "Sueldo",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        val transfer = Movement(
            id = "mov_tr_1",
            type = MovementType.TRANSFER,
            amount = Money.fromMajorUnits(200L),
            description = "Mesada",
            categoryId = Category.SYSTEM_ALLOCATION.id,
            sourceAccountRef = AccountReference.SHARED,
            destinationAccountRef = AccountReference.personal("mem_1"),
        )

        val family = Family(
            id = "fam_1",
            name = "Familia Pérez",
            members = listOf(member),
            movements = listOf(income, transfer),
        )

        assertEquals(2, family.movements.size)
        assertEquals("fam_1", family.id)
    }

    @Test
    fun testEmptyFamilyConstructionIsValid() {
        val emptyFamily = Family(id = "fam_empty", name = "Familia Vacía")
        assertEquals(0, emptyFamily.members.size)
        assertEquals(0, emptyFamily.savingsGoals.size)
        assertEquals(0, emptyFamily.movements.size)
        assertEquals(Money.ZERO, emptyFamily.totalFamilyMoney)
    }

    // --- 6. Operaciones dinámicas de dominio ---

    @Test
    fun testSharedToPersonalAllocationAndConservationOfTotalMoney() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val member = Member(id = "mem_1", name = "Carlos", role = MemberRole.PARENT)
        family = family.addMember(member)

        val income = Movement(
            id = "mov_inc_1",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(2_000_000L),
            description = "Sueldo",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(income)

        family = family.allocateToMember(
            memberId = "mem_1",
            amount = Money.fromMajorUnits(300_000L),
            description = "Mesada Carlos",
            categoryId = Category.SYSTEM_ALLOCATION.id,
            movementId = "mov_alloc_1",
        )

        val carlos = family.getMember("mem_1")!!
        assertEquals(Money.fromMajorUnits(1_700_000L), family.sharedAccount.totalMoney)
        assertEquals(Money.fromMajorUnits(300_000L), carlos.personalAccount.totalMoney)
        assertEquals(Money.fromMajorUnits(2_000_000L), family.totalFamilyMoney)
    }

    @Test
    fun testReturnFromMemberToSharedAccount() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val member = Member(id = "mem_1", name = "Ana", role = MemberRole.PARENT)
        family = family.addMember(member)

        val personalIncome = Movement(
            id = "mov_pinc_1",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(500_000L),
            description = "Trabajo independiente",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.personal("mem_1"),
        )
        family = family.recordPersonalIncome("mem_1", personalIncome)

        family = family.returnFromMember(
            memberId = "mem_1",
            amount = Money.fromMajorUnits(200_000L),
            description = "Aporte a gastos comunes",
            categoryId = Category.SYSTEM_ALLOCATION.id,
            movementId = "mov_ret_1",
        )

        val ana = family.getMember("mem_1")!!
        assertEquals(Money.fromMajorUnits(300_000L), ana.personalAccount.totalMoney)
        assertEquals(Money.fromMajorUnits(200_000L), family.sharedAccount.totalMoney)
        assertEquals(Money.fromMajorUnits(500_000L), family.totalFamilyMoney)
    }

    @Test
    fun testRejectionOfExpensesExceedingAvailableBalance() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val income = Movement(
            id = "mov_inc_1",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(100_000L),
            description = "Ingreso",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(income)

        val excessiveExpense = Movement(
            id = "mov_exp_1",
            type = MovementType.EXPENSE,
            amount = Money.fromMajorUnits(150_000L),
            description = "Gasto Excesivo",
            categoryId = Category.SYSTEM_EXPENSE.id,
            sourceAccountRef = AccountReference.SHARED,
        )

        assertFailsWith<InsufficientBalanceException> {
            family.recordSharedExpense(excessiveExpense)
        }
    }

    @Test
    fun testRejectionOfTransferUsingReservedSavings() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val member = Member(id = "mem_1", name = "Pedro", role = MemberRole.PARENT)
        family = family.addMember(member)

        val income = Movement(
            id = "mov_inc_p",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(500_000L),
            description = "Sueldo Pedro",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.personal("mem_1"),
        )
        family = family.recordPersonalIncome("mem_1", income)

        val goal = SavingsGoal(
            id = "goal_pedro",
            name = "Auto",
            targetAmount = Money.fromMajorUnits(1_000_000L),
            accountRef = AccountReference.personal("mem_1"),
        )
        family = family.addSavingsGoal(goal)
        family = family.depositToSavingsGoal("goal_pedro", Money.fromMajorUnits(400_000L), "mov_sav_1")

        assertFailsWith<InsufficientBalanceException> {
            family.returnFromMember(
                memberId = "mem_1",
                amount = Money.fromMajorUnits(200_000L),
                description = "Devolución excesiva",
                categoryId = Category.SYSTEM_ALLOCATION.id,
                movementId = "mov_ret_fail",
            )
        }
    }

    @Test
    fun testPreventionOfDuplicateMovementsOnDynamicOperations() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val income = Movement(
            id = "mov_unique_1",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(100_000L),
            description = "Ingreso Único",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(income)

        val duplicateIncome = Movement(
            id = "mov_unique_1",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(50_000L),
            description = "Ingreso Duplicado",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )

        assertFailsWith<IllegalArgumentException> {
            family.recordSharedIncome(duplicateIncome)
        }
    }

    @Test
    fun testRejectionOfDepositExceedingTargetAmount() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val sharedIncome = Movement(
            id = "mov_inc_sh",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(1_000_000L),
            description = "Ingreso Compartido",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(sharedIncome)

        val goal = SavingsGoal(
            id = "goal_target_500",
            name = "Viaje",
            targetAmount = Money.fromMajorUnits(500_000L),
            accountRef = AccountReference.SHARED,
        )
        family = family.addSavingsGoal(goal)

        assertFailsWith<DomainException> {
            family.depositToSavingsGoal("goal_target_500", Money.fromMajorUnits(600_000L), "mov_dep_excess")
        }
    }

    @Test
    fun testRejectionOfWithdrawalExceedingCurrentSavings() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val sharedIncome = Movement(
            id = "mov_inc_sh",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(1_000_000L),
            description = "Ingreso Compartido",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(sharedIncome)

        val goal = SavingsGoal(
            id = "goal_test",
            name = "Fondo",
            targetAmount = Money.fromMajorUnits(500_000L),
            accountRef = AccountReference.SHARED,
        )
        family = family.addSavingsGoal(goal)
        family = family.depositToSavingsGoal("goal_test", Money.fromMajorUnits(200_000L), "mov_dep_1")

        assertFailsWith<InsufficientSavingsException> {
            family.withdrawFromSavingsGoal("goal_test", Money.fromMajorUnits(300_000L), "mov_with_excess")
        }
    }

    @Test
    fun testValidSavingsOperationsKeepCoherenceWithAccountReserves() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val sharedIncome = Movement(
            id = "mov_inc_sh",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(1_000_000L),
            description = "Ingreso Compartido",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(sharedIncome)

        val goal = SavingsGoal(
            id = "goal_sh",
            name = "Fondo de Emergencia",
            targetAmount = Money.fromMajorUnits(800_000L),
            accountRef = AccountReference.SHARED,
        )
        family = family.addSavingsGoal(goal)

        family = family.depositToSavingsGoal("goal_sh", Money.fromMajorUnits(300_000L), "mov_dep_1")
        assertEquals(Money.fromMajorUnits(300_000L), family.sharedAccount.reservedSavings)

        family = family.withdrawFromSavingsGoal("goal_sh", Money.fromMajorUnits(100_000L), "mov_with_1")
        assertEquals(Money.fromMajorUnits(200_000L), family.sharedAccount.reservedSavings)
        assertEquals(Money.fromMajorUnits(800_000L), family.sharedAccount.availableMoney)
        assertEquals(Money.fromMajorUnits(1_000_000L), family.sharedAccount.totalMoney)
    }
}
