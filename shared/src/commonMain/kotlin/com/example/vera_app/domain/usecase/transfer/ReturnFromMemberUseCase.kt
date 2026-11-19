package com.example.vera_app.domain.usecase.transfer

import com.example.vera_app.domain.model.Category
import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.Money
import com.example.vera_app.domain.repository.FamilyRepository

/**
 * Caso de uso para devolver dinero desde la cuenta personal de un integrante a la cuenta compartida.
 */
class ReturnFromMemberUseCase(
    private val repository: FamilyRepository,
) {
    suspend operator fun invoke(
        familyId: String,
        memberId: String,
        amount: Money,
        description: String,
        categoryId: String = Category.SYSTEM_ALLOCATION.id,
        movementId: String,
        timestamp: Long = 0L,
    ): Family {
        val family = repository.getFamilyById(familyId)
            ?: throw FamilyNotFoundException(familyId)

        val updatedFamily = family.returnFromMember(
            memberId = memberId,
            amount = amount,
            description = description,
            categoryId = categoryId,
            movementId = movementId,
            timestamp = timestamp,
        )

        repository.saveFamily(updatedFamily)
        return updatedFamily
    }
}
