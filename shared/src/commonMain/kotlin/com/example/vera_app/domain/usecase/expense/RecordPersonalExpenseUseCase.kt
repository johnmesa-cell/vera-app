package com.example.vera_app.domain.usecase.expense

import com.example.vera_app.domain.model.AccountReference
import com.example.vera_app.domain.model.Category
import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.model.Movement
import com.example.vera_app.domain.model.MovementType
import com.example.vera_app.domain.repository.FamilyRepository

/**
 * Caso de uso para registrar un gasto desde la cuenta personal de un integrante.
 */
class RecordPersonalExpenseUseCase(
    private val repository: FamilyRepository,
) {
    suspend operator fun invoke(
        familyId: String,
        memberId: String,
        amount: Money,
        description: String,
        categoryId: String = Category.SYSTEM_EXPENSE.id,
        movementId: String,
        timestamp: Long = 0L,
    ): Family {
        val family = repository.getFamilyById(familyId)
            ?: throw FamilyNotFoundException(familyId)

        val movement = Movement(
            id = movementId,
            type = MovementType.EXPENSE,
            amount = amount,
            description = description,
            categoryId = categoryId,
            sourceAccountRef = AccountReference.personal(memberId),
            timestamp = timestamp,
        )

        val updatedFamily = family.recordPersonalExpense(memberId, movement)
        repository.saveFamily(updatedFamily)
        return updatedFamily
    }

    suspend operator fun invoke(
        familyId: String,
        memberId: String,
        movement: Movement,
    ): Family {
        val family = repository.getFamilyById(familyId)
            ?: throw FamilyNotFoundException(familyId)

        val updatedFamily = family.recordPersonalExpense(memberId, movement)
        repository.saveFamily(updatedFamily)
        return updatedFamily
    }
}
