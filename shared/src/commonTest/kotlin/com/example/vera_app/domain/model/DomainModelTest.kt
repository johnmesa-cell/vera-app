package com.example.vera_app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DomainModelTest {

    @Test
    fun testMoneyOperations() {
        val m100 = Money(100L)
        val m50 = Money(50L)

        assertEquals(Money(150L), m100 + m50)
        assertEquals(Money(50L), m100 - m50)
        assertTrue(m100 > m50)

        assertFailsWith<IllegalArgumentException> {
            Money(-10L)
        }
    }

    @Test
    fun testSharedAccountIncomeAndExpense() {
        val family = Family(id = "fam_1", name = "Familia Pérez")

        val incomeMovement = Movement(
            id = "mov_1",
            type = MovementType.INCOME,
            amount = Money(2_000_000L),
            description = "Sueldo mensual",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )

        val familyWithIncome = family.recordSharedIncome(incomeMovement)

        assertEquals(Money(2_000_000L), familyWithIncome.sharedAccount.totalMoney)
        assertEquals(Money(2_000_000L), familyWithIncome.sharedAccount.availableMoney)
        assertEquals(Money(2_000_000L), familyWithIncome.totalFamilyMoney)

        val expenseMovement = Movement(
            id = "mov_2",
            type = MovementType.EXPENSE,
            amount = Money(500_000L),
            description = "Supermercado",
            categoryId = Category.SYSTEM_EXPENSE.id,
            sourceAccountRef = AccountReference.SHARED,
        )

        val familyAfterExpense = familyWithIncome.recordSharedExpense(expenseMovement)

        assertEquals(Money(1_500_000L), familyAfterExpense.sharedAccount.totalMoney)
        assertEquals(Money(1_500_000L), familyAfterExpense.sharedAccount.availableMoney)
        assertEquals(Money(1_500_000L), familyAfterExpense.totalFamilyMoney)
    }

    @Test
    fun testAllocationToMember() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val member = Member(id = "mem_1", name = "Juan", role = MemberRole.PARENT)
        family = family.addMember(member)

        val incomeMovement = Movement(
            id = "mov_1",
            type = MovementType.INCOME,
            amount = Money(2_000_000L),
            description = "Ingreso Familiar",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(incomeMovement)

        // Asignación de 300.000 a Juan
        family = family.allocateToMember(
            memberId = "mem_1",
            amount = Money(300_000L),
            description = "Asignación mensual Juan",
            categoryId = Category.SYSTEM_ALLOCATION.id,
            movementId = "mov_alloc_1",
        )

        val updatedMember = family.getMember("mem_1")
        assertNotNull(updatedMember)

        // El saldo de la cuenta compartida disminuye en 300.000 (queda 1.700.000)
        assertEquals(Money(1_700_000L), family.sharedAccount.totalMoney)
        assertEquals(Money(1_700_000L), family.sharedAccount.availableMoney)

        // El saldo del integrante aumenta en 300.000
        assertEquals(Money(300_000L), updatedMember.personalAccount.totalMoney)
        assertEquals(Money(300_000L), updatedMember.personalAccount.availableMoney)

        // El dinero TOTAL de la familia NO cambia (sigue siendo 2.000.000)
        assertEquals(Money(2_000_000L), family.totalFamilyMoney)
        assertEquals(Money(2_000_000L), family.totalFamilyAvailableMoney)
        assertEquals(Money(300_000L), family.totalAllocatedToMembers)
    }

    @Test
    fun testSavingsReservations() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val member = Member(id = "mem_1", name = "María", role = MemberRole.PARENT)
        family = family.addMember(member)

        // Ingreso familiar: 2.000.000
        val incomeMovement = Movement(
            id = "mov_1",
            type = MovementType.INCOME,
            amount = Money(2_000_000L),
            description = "Ingreso Familiar",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(incomeMovement)

        // Meta de ahorro familiar: 500.000
        val sharedGoal = SavingsGoal(
            id = "goal_fam",
            name = "Vacaciones Familiares",
            targetAmount = Money(500_000L),
            accountRef = AccountReference.SHARED,
        )
        family = family.addSavingsGoal(sharedGoal)

        // Reserva 300.000 para ahorro familiar
        family = family.depositToSavingsGoal("goal_fam", Money(300_000L), "mov_sav_1")

        // Verificación de dinero total vs disponible familiar
        assertEquals(Money(2_000_000L), family.sharedAccount.totalMoney) // No disminuye el total
        assertEquals(Money(300_000L), family.sharedAccount.reservedSavings)
        assertEquals(Money(1_700_000L), family.sharedAccount.availableMoney) // Solo disminuye el disponible

        // Asignar 500.000 a María y ella reserva 100.000 para su ahorro personal
        family = family.allocateToMember("mem_1", Money(500_000L), "Asignación María", Category.SYSTEM_ALLOCATION.id, "mov_alloc_2")

        val personalGoal = SavingsGoal(
            id = "goal_mem",
            name = "Curso de Fotografía",
            targetAmount = Money(200_000L),
            accountRef = AccountReference.personal("mem_1"),
        )
        family = family.addSavingsGoal(personalGoal)

        family = family.depositToSavingsGoal("goal_mem", Money(100_000L), "mov_sav_2")

        val maria = family.getMember("mem_1")!!
        assertEquals(Money(500_000L), maria.personalAccount.totalMoney)
        assertEquals(Money(100_000L), maria.personalAccount.reservedSavings)
        assertEquals(Money(400_000L), maria.personalAccount.availableMoney)

        // Totales consolidados de la familia
        assertEquals(Money(2_000_000L), family.totalFamilyMoney)
        assertEquals(Money(400_000L), family.totalFamilyReservedSavings) // 300.000 + 100.000
        assertEquals(Money(1_600_000L), family.totalFamilyAvailableMoney) // 1.200.000 (compartido disponible) + 400.000 (personal disponible)
    }

    @Test
    fun testValidationExceedAvailableBalance() {
        var family = Family(id = "fam_1", name = "Familia Pérez")
        val incomeMovement = Movement(
            id = "mov_1",
            type = MovementType.INCOME,
            amount = Money(100_000L),
            description = "Ingreso",
            categoryId = Category.SYSTEM_INCOME.id,
            destinationAccountRef = AccountReference.SHARED,
        )
        family = family.recordSharedIncome(incomeMovement)

        // Intentar gastar más de lo disponible debe fallar
        val excessiveExpense = Movement(
            id = "mov_2",
            type = MovementType.EXPENSE,
            amount = Money(150_000L),
            description = "Gasto Excesivo",
            categoryId = Category.SYSTEM_EXPENSE.id,
            sourceAccountRef = AccountReference.SHARED,
        )

        assertFailsWith<InsufficientBalanceException> {
            family.recordSharedExpense(excessiveExpense)
        }
    }

    @Test
    fun testValidationInvalidAmount() {
        assertFailsWith<InvalidAmountException> {
            Movement(
                id = "mov_inv",
                type = MovementType.INCOME,
                amount = Money(0L),
                description = "Inválido",
                categoryId = Category.SYSTEM_INCOME.id,
                destinationAccountRef = AccountReference.SHARED,
            )
        }
    }
}
