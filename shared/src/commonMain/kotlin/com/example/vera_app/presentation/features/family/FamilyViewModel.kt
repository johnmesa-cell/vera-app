package com.example.vera_app.presentation.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vera_app.domain.model.DomainException
import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.model.FamilyAlreadyExistsException
import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.family.GetFamilyUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de la gestión y consulta de la información general de una familia.
 *
 * Expone su estado mediante un [StateFlow] inmutable ([FamilyUiState]) y coordina las operaciones
 * utilizando los casos de uso del dominio [GetFamilyUseCase] y [CreateFamilyUseCase].
 *
 * @property getFamilyUseCase Caso de uso para obtener una familia por ID.
 * @property createFamilyUseCase Caso de uso para registrar una nueva familia.
 */
class FamilyViewModel(
    private val getFamilyUseCase: GetFamilyUseCase,
    private val createFamilyUseCase: CreateFamilyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    private var lastQueriedFamilyId: String? = null

    /**
     * Consulta una familia por su identificador único.
     *
     * @param id Identificador de la familia a buscar.
     */
    fun loadFamily(id: String) {
        if (id.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "El identificador de la familia no puede estar vacío.",
                    isLoading = false,
                )
            }
            return
        }

        lastQueriedFamilyId = id

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isCreatedSuccessfully = false
                )
            }
            try {
                val family = getFamilyUseCase(id)
                _uiState.update {
                    it.copy(
                        family = family,
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: FamilyNotFoundException) {
                _uiState.update {
                    it.copy(
                        family = null,
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = null
                    )
                }
            } catch (e: DomainException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = e.message ?: "Ocurrió un error al consultar la familia."
                    )
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = e.message ?: "Parámetro inválido al consultar la familia."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = "Error inesperado al consultar la familia."
                    )
                }
            }
        }
    }

    /**
     * Crea una nueva familia con el identificador y nombre especificados.
     *
     * @param id Identificador de la nueva familia.
     * @param name Nombre de la nueva familia.
     */
    fun createFamily(id: String, name: String) {
        if (id.isBlank() || name.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "El ID y el nombre de la familia son requeridos.",
                    isLoading = false,
                    isCreatedSuccessfully = false
                )
            }
            return
        }

        lastQueriedFamilyId = id

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isCreatedSuccessfully = false
                )
            }
            try {
                val createdFamily = createFamilyUseCase(id, name)
                _uiState.update {
                    it.copy(
                        family = createdFamily,
                        isLoading = false,
                        hasSearched = true,
                        isCreatedSuccessfully = true,
                        errorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: FamilyAlreadyExistsException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreatedSuccessfully = false,
                        errorMessage = e.message ?: "Ya existe una familia registrada con el ID: $id"
                    )
                }
            } catch (e: DomainException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreatedSuccessfully = false,
                        errorMessage = e.message ?: "Ocurrió un error al crear la familia."
                    )
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreatedSuccessfully = false,
                        errorMessage = e.message ?: "Datos de familia inválidos."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreatedSuccessfully = false,
                        errorMessage = "Error inesperado al crear la familia."
                    )
                }
            }
        }
    }

    /**
     * Crea una nueva familia aceptando directamente un objeto [Family].
     *
     * @param family Instancia de [Family] a registrar.
     */
    fun createFamily(family: Family) {
        lastQueriedFamilyId = family.id

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isCreatedSuccessfully = false
                )
            }
            try {
                val createdFamily = createFamilyUseCase(family)
                _uiState.update {
                    it.copy(
                        family = createdFamily,
                        isLoading = false,
                        hasSearched = true,
                        isCreatedSuccessfully = true,
                        errorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: FamilyAlreadyExistsException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreatedSuccessfully = false,
                        errorMessage = e.message ?: "Ya existe una familia registrada con el ID: ${family.id}"
                    )
                }
            } catch (e: DomainException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreatedSuccessfully = false,
                        errorMessage = e.message ?: "Ocurrió un error al crear la familia."
                    )
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreatedSuccessfully = false,
                        errorMessage = e.message ?: "Datos de familia inválidos."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreatedSuccessfully = false,
                        errorMessage = "Error inesperado al crear la familia."
                    )
                }
            }
        }
    }

    /**
     * Vuelve a consultar la información de la familia actualmente cargada o buscada previamente.
     */
    fun refresh() {
        val idToQuery = lastQueriedFamilyId ?: uiState.value.family?.id
        if (idToQuery != null) {
            loadFamily(idToQuery)
        }
    }

    /**
     * Limpia el mensaje de error del estado actual.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
