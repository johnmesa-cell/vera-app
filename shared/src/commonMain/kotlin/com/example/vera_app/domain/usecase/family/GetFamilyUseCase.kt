package com.example.vera_app.domain.usecase.family

import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.repository.FamilyRepository

/**
 * Caso de uso para consultar una familia por su identificador único.
 */
class GetFamilyUseCase(
    private val repository: FamilyRepository,
) {
    suspend operator fun invoke(id: String): Family? {
        return repository.getFamilyById(id)
    }
}
