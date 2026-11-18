package com.example.vera_app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DomainModelTest {

    @Test
    fun testMoneyConversionAndUnits() {
        val hundredPesos = Money.fromMajorUnits(100L)
        assertEquals(10_000L, hundredPesos.toCents())
        assertEquals(100L, hundredPesos.toMajorUnits())

        val fiftyCents = Money.of(50L)
        assertEquals(50L, fiftyCents.toCents())
        assertEquals(0L, fiftyCents.toMajorUnits())
    }

    @Test
    fun testMoneyArithmeticAndComparisons() {
        val m1 = Money.fromMajorUnits(100L) // 10.000 centavos
        val m2 = Money.fromMajorUnits(50L)  // 5.000 centavos

        assertEquals(Money.fromMajorUnits(150L), m1 + m2)
        assertEquals(Money.fromMajorUnits(50L), m1 - m2)
        assertTrue(m1 > m2)

        assertFailsWith<IllegalArgumentException> {
            Money.of(-100L)
        }
        assertFailsWith<IllegalArgumentException> {
            Money.fromMajorUnits(-5L)
        }
    }

    @Test
    fun testRejectionOfInvalidAmounts() {
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

    @Test
    fun testSharedToPersonalAllocationAndConservationOfTotalMoney() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val member = Member(id = "mem_1", name = "Carlos", role = MemberRole.PARENT)
        family = family.addMember(member)

        // Ingreso familiar: $2.000.000 (en centavos: 200.000.000)
        val income = Movement(
            id = "mov_inc_1",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(2_000_000L),
            description = "Sueldo",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(income)

        // Asignación a Carlos: $300.000
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

        // Verificación de conservación de dinero total familiar
        assertEquals(Money.fromMajorUnits(2_000_000L), family.totalFamilyMoney)
    }

    @Test
    fun testReturnFromMemberToSharedAccount() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val member = Member(id = "mem_1", name = "Ana", role = MemberRole.PARENT)
        family = family.addMember(member)

        // Ingreso personal directo a Ana: $500.000
        val personalIncome = Movement(
            id = "mov_pinc_1",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(500_000L),
            description = "Trabajo independiente",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.personal("mem_1"),
        )
        family = family.recordPersonalIncome("mem_1", personalIncome)

        // Devolución de Ana a la cuenta compartida: $200.000
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

        // Conservación del dinero total familiar
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

        // Ingreso a Pedro: $500.000
        val income = Movement(
            id = "mov_inc_p",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(500_000L),
            description = "Sueldo Pedro",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.personal("mem_1"),
        )
        family = family.recordPersonalIncome("mem_1", income)

        // Reserva $400.000 para ahorro personal de Pedro (Disponible: $100.000)
        val goal = SavingsGoal(
            id = "goal_pedro",
            name = "Auto",
            targetAmount = Money.fromMajorUnits(1_000_000L),
            accountRef = AccountReference.personal("mem_1"),
        )
        family = family.addSavingsGoal(goal)
        family = family.depositToSavingsGoal("goal_pedro", Money.fromMajorUnits(400_000L), "mov_sav_1")

        // Intentar devolver $200.000 a la cuenta compartida debe fallar porque solo hay $100.000 disponibles
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
    fun testPreventionOfDuplicateMovements() {
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

        // Registrar un segundo movimiento con el mismo ID debe fallar
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
    fun testSavingsGoalValidationsAndIntegrity() {
        // Meta con objetivo cero debe fallar
        assertFailsWith<IllegalArgumentException> {
            SavingsGoal(
                id = "goal_zero",
                name = "Inválida",
                targetAmount = Money.ZERO,
                accountRef = AccountReference.SHARED,
            )
        }

        // Meta con saldo inicial mayor a cero debe rechazarse al agregarla a la familia
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val invalidInitialGoal = SavingsGoal(
            id = "goal_init",
            name = "Con Saldo Inicial Sin Reservar",
            targetAmount = Money.fromMajorUnits(500_000L),
            currentAmount = Money.fromMajorUnits(100_000L),
            accountRef = AccountReference.SHARED,
        )
        assertFailsWith<IllegalArgumentException> {
            family.addSavingsGoal(invalidInitialGoal)
        }
    }

    @Test
    fun testSavingsDepositsWithdrawalsAndIntegrityWithAccounts() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val member = Member(id = "mem_1", name = "Lucía", role = MemberRole.PARENT)
        family = family.addMember(member)

        val sharedIncome = Movement(
            id = "mov_inc_sh",
            type = MovementType.INCOME,
            amount = Money.fromMajorUnits(1_000_000L),
            description = "Ingreso Compartido",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(sharedIncome)

        val goalShared = SavingsGoal(
            id = "goal_sh",
            name = "Fondo de Emergencia",
            targetAmount = Money.fromMajorUnits(800_000L),
            accountRef = AccountReference.SHARED,
        )
        family = family.addSavingsGoal(goalShared)

        // Depositar $300.000 en la meta de ahorro compartida
        family = family.depositToSavingsGoal("goal_sh", Money.fromMajorUnits(300_000L), "mov_dep_1")

        // Coherencia: la suma de metas del objetivo coincide exactamente con `reservedSavings` de la cuenta
        val currentGoal = family.getSavingsGoal("goal_sh")!!
        assertEquals(Money.fromMajorUnits(300_000L), currentGoal.currentAmount)
        assertEquals(Money.fromMajorUnits(300_000L), family.sharedAccount.reservedSavings)
        assertEquals(Money.fromMajorUnits(700_000L), family.sharedAccount.availableMoney)
        assertEquals(Money.fromMajorUnits(1_000_000L), family.sharedAccount.totalMoney) // Total intacto

        // Retirar $100.000 de la meta
        family = family.withdrawFromSavingsGoal("goal_sh", Money.fromMajorUnits(100_000L), "mov_with_1")

        val updatedGoal = family.getSavingsGoal("goal_sh")!!
        assertEquals(Money.fromMajorUnits(200_000L), updatedGoal.currentAmount)
        assertEquals(Money.fromMajorUnits(200_000L), family.sharedAccount.reservedSavings)
        assertEquals(Money.fromMajorUnits(800_000L), family.sharedAccount.availableMoney)
        assertEquals(Money.fromMajorUnits(1_000_000L), family.sharedAccount.totalMoney)
    }
}
