package com.example.vera_app.domain.usecase.savings

import com.example.vera_app.domain.model.AccountReference
import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.model.SavingsGoal
import com.example.vera_app.domain.repository.FamilyRepository

/**
 * Caso de uso para crear una nueva meta de ahorro (compartida o personal).
 */
class CreateSavingsGoalUseCase(
    private val repository: FamilyRepository,
) {
    suspend operator fun invoke(
        familyId: String,
        id: String,
        name: String,
        targetAmount: Money,
        accountRef: AccountReference,
    ): Family {
        val family = repository.getFamilyById(familyId)
            ?: throw FamilyNotFoundException(familyId)

        val savingsGoal = SavingsGoal(
            id = id,
            name = name,
            targetAmount = targetAmount,
            currentAmount = Money.ZERO,
            accountRef = accountRef,
        )

        val updatedFamily = family.addSavingsGoal(savingsGoal)
        repository.saveFamily(updatedFamily)
        return updatedFamily
    }

    suspend operator fun invoke(
        familyId: String,
        savingsGoal: SavingsGoal,
    ): Family {
        val family = repository.getFamilyById(familyId)
            ?: throw FamilyNotFoundException(familyId)

        val updatedFamily = family.addSavingsGoal(savingsGoal)
        repository.saveFamily(updatedFamily)
        return updatedFamily
    }
}
