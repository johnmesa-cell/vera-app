package com.example.vera_app.domain.usecase.savings

import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.repository.FamilyRepository

/**
 * Caso de uso para depositar dinero reservado en una meta de ahorro.
 */
class DepositToSavingsGoalUseCase(
    private val repository: FamilyRepository,
) {
    suspend operator fun invoke(
        familyId: String,
        savingsGoalId: String,
        amount: Money,
        movementId: String,
        timestamp: Long = 0L,
    ): Family {
        val family = repository.getFamilyById(familyId)
            ?: throw FamilyNotFoundException(familyId)

        val updatedFamily = family.depositToSavingsGoal(
            savingsGoalId = savingsGoalId,
            amount = amount,
            movementId = movementId,
            timestamp = timestamp,
        )

        repository.saveFamily(updatedFamily)
        return updatedFamily
    }
}
