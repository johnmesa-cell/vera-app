package com.example.vera_app.presentation.features.family

import com.example.vera_app.domain.model.Family

/**
 * Estado inmutable de la interfaz de usuario para la pantalla o sección de gestión familiar.
 *
 * @property family Instancia de [Family] consultada o creada exitosamente, o `null` si aún no existe o no se ha cargado.
 * @property isLoading Indica si hay una operación asíncrona en desarrollo (carga, creación o actualización).
 * @property hasSearched Indica si se ha realizado al menos una consulta de familia.
 *                       Permite diferenciar un estado inicial (sin consultar) de un resultado que confirma que la familia no existe.
 * @property isCreatedSuccessfully Indica si la última operación de creación de familia finalizó con éxito.
 * @property errorMessage Mensaje de error formateado para ser presentado a la UI, o `null` si no hay error activo.
 */
data class FamilyUiState(
    val family: Family? = null,
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val isCreatedSuccessfully: Boolean = false,
    val errorMessage: String? = null,
) {
    /**
     * Propiedad calculada que indica si se ha ejecutado una búsqueda y la familia no fue encontrada.
     */
    val isFamilyNotFound: Boolean
        get() = hasSearched && (family == null)
}
