package com.example.vera_app.domain.usecase.member

import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.Member
import com.example.vera_app.domain.repository.FamilyRepository

/**
 * Caso de uso para agregar un integrante a una familia existente.
 */
class AddMemberUseCase(
    private val repository: FamilyRepository,
) {
    suspend operator fun invoke(familyId: String, member: Member): Family {
        val family = repository.getFamilyById(familyId)
            ?: throw FamilyNotFoundException(familyId)

        val updatedFamily = family.addMember(member)
        repository.saveFamily(updatedFamily)
        return updatedFamily
    }
}
