package com.example.vera_app.domain.usecase.income

import com.example.vera_app.domain.model.AccountReference
import com.example.vera_app.domain.model.Category
import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.model.Movement
import com.example.vera_app.domain.model.MovementType
import com.example.vera_app.domain.repository.FamilyRepository

/**
 * Caso de uso para registrar un ingreso en la cuenta compartida familiar.
 */
class RecordSharedIncomeUseCase(
    private val repository: FamilyRepository,
) {
    suspend operator fun invoke(
        familyId: String,
        amount: Money,
        description: String,
        categoryId: String = Category.SYSTEM_INCOME.id,
        movementId: String,
        timestamp: Long = 0L,
    ): Family {
        val family = repository.getFamilyById(familyId)
            ?: throw FamilyNotFoundException(familyId)

        val movement = Movement(
            id = movementId,
            type = MovementType.INCOME,
            amount = amount,
            description = description,
            categoryId = categoryId,
            destinationAccountRef = AccountReference.SHARED,
            timestamp = timestamp,
        )

        val updatedFamily = family.recordSharedIncome(movement)
        repository.saveFamily(updatedFamily)
        return updatedFamily
    }

    suspend operator fun invoke(familyId: String, movement: Movement): Family {
        val family = repository.getFamilyById(familyId)
            ?: throw FamilyNotFoundException(familyId)

        val updatedFamily = family.recordSharedIncome(movement)
        repository.saveFamily(updatedFamily)
        return updatedFamily
    }
}
