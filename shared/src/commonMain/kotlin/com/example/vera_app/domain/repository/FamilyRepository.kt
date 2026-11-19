package com.example.vera_app.domain.repository

import com.example.vera_app.domain.model.Family

/**
 * Contrato del repositorio para la consulta y persistencia del agregado [Family].
 */
interface FamilyRepository {
    /**
     * Obtiene una familia mediante su identificador único.
     *
     * @param id Identificador de la familia.
     * @return La instancia de [Family] si existe, o `null` si no se encuentra.
     */
    suspend fun getFamilyById(id: String): Family?

    /**
     * Crea una nueva familia en la persistencia.
     *
     * @param family La instancia de [Family] a registrar.
     */
    suspend fun createFamily(family: Family)

    /**
     * Guarda los cambios de una familia existente en la persistencia.
     *
     * @param family La instancia de [Family] modificada a actualizar.
     */
    suspend fun saveFamily(family: Family)
}
