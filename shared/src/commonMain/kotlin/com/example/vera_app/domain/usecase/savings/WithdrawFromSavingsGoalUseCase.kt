package com.example.vera_app.domain.usecase.savings

import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.repository.FamilyRepository

/**
 * Caso de uso para retirar dinero de una meta de ahorro de vuelta al saldo disponible.
 */
class WithdrawFromSavingsGoalUseCase(
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

        val updatedFamily = family.withdrawFromSavingsGoal(
            savingsGoalId = savingsGoalId,
            amount = amount,
            movementId = movementId,
            timestamp = timestamp,
        )

        repository.saveFamily(updatedFamily)
        return updatedFamily
    }
}
