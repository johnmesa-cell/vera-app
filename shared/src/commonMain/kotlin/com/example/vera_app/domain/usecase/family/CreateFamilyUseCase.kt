package com.example.vera_app.domain.usecase.family

import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.model.FamilyAlreadyExistsException
import com.example.vera_app.domain.repository.FamilyRepository

/**
 * Caso de uso para crear una nueva familia en el sistema.
 */
class CreateFamilyUseCase(
    private val repository: FamilyRepository,
) {
    suspend operator fun invoke(id: String, name: String): Family {
        val existing = repository.getFamilyById(id)
        if (existing != null) {
            throw FamilyAlreadyExistsException(id)
        }
        val family = Family(id = id, name = name)
        repository.createFamily(family)
        return family
    }

    suspend operator fun invoke(family: Family): Family {
        val existing = repository.getFamilyById(family.id)
        if (existing != null) {
            throw FamilyAlreadyExistsException(family.id)
        }
        repository.createFamily(family)
        return family
    }
}
