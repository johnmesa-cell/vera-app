package com.example.vera_app.presentation.features.family

import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.repository.FamilyRepository
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.family.GetFamilyUseCase
import com.example.vera_app.repository.FakeFamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeFamilyRepository
    private lateinit var getFamilyUseCase: GetFamilyUseCase
    private lateinit var createFamilyUseCase: CreateFamilyUseCase
    private lateinit var viewModel: FamilyViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeFamilyRepository()
        getFamilyUseCase = GetFamilyUseCase(repository)
        createFamilyUseCase = CreateFamilyUseCase(repository)
        viewModel = FamilyViewModel(getFamilyUseCase, createFamilyUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() {
        val state = viewModel.uiState.value
        assertNull(state.family)
        assertFalse(state.isLoading)
        assertFalse(state.hasSearched)
        assertFalse(state.isCreatedSuccessfully)
        assertNull(state.errorMessage)
        assertFalse(state.isFamilyNotFound)
    }

    @Test
    fun testLoadFamilySuccess() = runTest {
        repository.createFamily(Family("fam_1", "Familia Pérez"))

        viewModel.loadFamily("fam_1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val family = assertNotNull(state.family)
        assertEquals("fam_1", family.id)
        assertEquals("Familia Pérez", family.name)
        assertFalse(state.isLoading)
        assertTrue(state.hasSearched)
        assertNull(state.errorMessage)
        assertFalse(state.isFamilyNotFound)
    }

    @Test
    fun testLoadFamilyNotFound() = runTest {
        viewModel.loadFamily("fam_non_existent")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.family)
        assertFalse(state.isLoading)
        assertTrue(state.hasSearched)
        assertNull(state.errorMessage)
        assertTrue(state.isFamilyNotFound)
    }

    @Test
    fun testLoadFamilyWithBlankIdSetsErrorAndDoesNotFlagNotFound() = runTest {
        viewModel.loadFamily("   ")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasSearched)
        assertNotNull(state.errorMessage)
        assertEquals("El identificador de la familia no puede estar vacío.", state.errorMessage)
        assertFalse(state.isFamilyNotFound)
    }

    @Test
    fun testLoadFamilyHandlesRepositoryErrorAndDoesNotFlagNotFound() = runTest {
        val failingRepo = object : FamilyRepository {
            override suspend fun getFamilyById(id: String): Family? {
                throw IllegalStateException("Error de conexión con la base de datos.")
            }

            override suspend fun createFamily(family: Family) {}
            override suspend fun saveFamily(family: Family) {}
        }
        val failingViewModel = FamilyViewModel(
            GetFamilyUseCase(failingRepo),
            CreateFamilyUseCase(failingRepo),
        )

        failingViewModel.loadFamily("fam_1")
        advanceUntilIdle()

        val state = failingViewModel.uiState.value
        assertNull(state.family)
        assertFalse(state.isLoading)
        assertFalse(state.hasSearched)
        assertEquals("Error inesperado al consultar la familia.", state.errorMessage)
        assertFalse(state.isFamilyNotFound)
    }

    @Test
    fun testIsFamilyNotFoundEvaluationRules() = runTest {
        // 1. Sin consultar previamente -> isFamilyNotFound es false
        assertFalse(viewModel.uiState.value.isFamilyNotFound)

        // 2. Consulta exitosa de familia inexistente -> isFamilyNotFound es true
        viewModel.loadFamily("inexistente")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFamilyNotFound)

        // 3. Validación de parámetro inválido -> isFamilyNotFound es false
        viewModel.loadFamily("  ")
        assertFalse(viewModel.uiState.value.hasSearched)
        assertFalse(viewModel.uiState.value.isFamilyNotFound)

        // 4. Fallo por error técnico -> isFamilyNotFound es false
        val failingRepo = object : FamilyRepository {
            override suspend fun getFamilyById(id: String): Family? {
                throw RuntimeException("Fallo en la BD")
            }

            override suspend fun createFamily(family: Family) {}
            override suspend fun saveFamily(family: Family) {}
        }
        val failingViewModel = FamilyViewModel(
            GetFamilyUseCase(failingRepo),
            CreateFamilyUseCase(failingRepo),
        )

        failingViewModel.loadFamily("fam_error")
        advanceUntilIdle()
        assertFalse(failingViewModel.uiState.value.hasSearched)
        assertFalse(failingViewModel.uiState.value.isFamilyNotFound)
    }

    @Test
    fun testCreateFamilySuccessByIdAndName() = runTest {
        viewModel.createFamily("fam_100", "Familia Rodríguez")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val family = assertNotNull(state.family)
        assertEquals("fam_100", family.id)
        assertEquals("Familia Rodríguez", family.name)
        assertFalse(state.isLoading)
        assertFalse(state.hasSearched)
        assertTrue(state.isCreatedSuccessfully)
        assertNull(state.errorMessage)

        val inRepo = repository.getFamilyById("fam_100")
        assertNotNull(inRepo)
        assertEquals("Familia Rodríguez", inRepo.name)
    }

    @Test
    fun testCreateFamilySuccessByObject() = runTest {
        val familyToCreate = Family("fam_200", "Familia López")
        viewModel.createFamily(familyToCreate)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val family = assertNotNull(state.family)
        assertEquals("fam_200", family.id)
        assertFalse(state.isLoading)
        assertTrue(state.isCreatedSuccessfully)
        assertNull(state.errorMessage)
    }

    @Test
    fun testCreateFamilyFailsWhenDuplicateId() = runTest {
        repository.createFamily(Family("fam_1", "Familia Existente"))

        viewModel.createFamily("fam_1", "Familia Nueva Duplicada")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isCreatedSuccessfully)
        val errorMessage = assertNotNull(state.errorMessage)
        assertTrue(errorMessage.contains("fam_1"))

        val originalInRepo = repository.getFamilyById("fam_1")
        assertEquals("Familia Existente", originalInRepo?.name)
    }

    @Test
    fun testCreateFamilyWithBlankParametersSetsError() = runTest {
        viewModel.createFamily("", "Familia Varia")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isCreatedSuccessfully)
        assertNotNull(state.errorMessage)
        assertEquals("El ID y el nombre de la familia son requeridos.", state.errorMessage)
    }

    @Test
    fun testCreateFamilyWithBlankNameInDomainSetsError() = runTest {
        viewModel.createFamily("fam_300", "   ")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isCreatedSuccessfully)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun testCreateFamilyHandlesUnexpectedError() = runTest {
        val failingRepo = object : FamilyRepository {
            override suspend fun getFamilyById(id: String): Family? = null
            override suspend fun createFamily(family: Family) {
                throw RuntimeException("Fallo al escribir en almacenamiento.")
            }

            override suspend fun saveFamily(family: Family) {}
        }
        val failingViewModel = FamilyViewModel(
            GetFamilyUseCase(failingRepo),
            CreateFamilyUseCase(failingRepo),
        )

        failingViewModel.createFamily("fam_400", "Familia Error")
        advanceUntilIdle()

        val state = failingViewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isCreatedSuccessfully)
        assertEquals("Error inesperado al crear la familia.", state.errorMessage)
    }

    @Test
    fun testLoadingStateDuringLoadOperation() = runTest {
        val delayedRepo = object : FamilyRepository {
            override suspend fun getFamilyById(id: String): Family? {
                delay(100)
                return Family(id, "Familia Con Retraso")
            }

            override suspend fun createFamily(family: Family) {}
            override suspend fun saveFamily(family: Family) {}
        }
        val delayedViewModel = FamilyViewModel(
            GetFamilyUseCase(delayedRepo),
            CreateFamilyUseCase(delayedRepo),
        )

        delayedViewModel.loadFamily("fam_1")
        testScheduler.runCurrent()

        assertTrue(delayedViewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val stateFinished = delayedViewModel.uiState.value
        assertFalse(stateFinished.isLoading)
        assertNotNull(stateFinished.family)
    }

    @Test
    fun testLoadingStateDuringCreateOperation() = runTest {
        val delayedRepo = object : FamilyRepository {
            override suspend fun getFamilyById(id: String): Family? = null
            override suspend fun createFamily(family: Family) {
                delay(100)
            }

            override suspend fun saveFamily(family: Family) {}
        }
        val delayedViewModel = FamilyViewModel(
            GetFamilyUseCase(delayedRepo),
            CreateFamilyUseCase(delayedRepo),
        )

        delayedViewModel.createFamily("fam_1", "Familia Con Retraso")
        testScheduler.runCurrent()

        assertTrue(delayedViewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val stateFinished = delayedViewModel.uiState.value
        assertFalse(stateFinished.isLoading)
        assertTrue(stateFinished.isCreatedSuccessfully)
    }

    @Test
    fun testRefreshReloadsLastQueriedFamily() = runTest {
        repository.createFamily(Family("fam_1", "Familia Inicial"))

        viewModel.loadFamily("fam_1")
        advanceUntilIdle()
        assertEquals("Familia Inicial", viewModel.uiState.value.family?.name)

        // Modificamos externamente la familia en el repositorio para simular un cambio de estado
        repository.saveFamily(Family("fam_1", "Familia Actualizada"))

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("Familia Actualizada", viewModel.uiState.value.family?.name)
    }

    @Test
    fun testClearErrorRemovesErrorMessage() = runTest {
        viewModel.loadFamily("")
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testBlankIdDoesNotPreservePreviousNotFoundResult() = runTest {
        viewModel.loadFamily("fam_non_existent")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFamilyNotFound)

        viewModel.loadFamily(" ")

        assertFalse(viewModel.uiState.value.hasSearched)
        assertFalse(viewModel.uiState.value.isFamilyNotFound)
    }

    @Test
    fun testClearErrorDoesNotRestorePreviousNotFoundResult() = runTest {
        viewModel.loadFamily("fam_non_existent")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFamilyNotFound)

        viewModel.loadFamily("")
        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.clearError()

        assertFalse(viewModel.uiState.value.hasSearched)
        assertFalse(viewModel.uiState.value.isFamilyNotFound)
    }

    @Test
    fun testNewQueryClearsPreviousNotFoundResultWhileLoading() = runTest {
        var response: Family? = null
        var shouldDelay = false
        val controllableRepo = object : FamilyRepository {
            override suspend fun getFamilyById(id: String): Family? {
                if (shouldDelay) {
                    delay(100)
                }
                return response
            }

            override suspend fun createFamily(family: Family) {}
            override suspend fun saveFamily(family: Family) {}
        }
        val controllableViewModel = FamilyViewModel(
            GetFamilyUseCase(controllableRepo),
            CreateFamilyUseCase(controllableRepo),
        )

        controllableViewModel.loadFamily("fam_non_existent")
        advanceUntilIdle()
        assertTrue(controllableViewModel.uiState.value.hasSearched)
        assertTrue(controllableViewModel.uiState.value.isFamilyNotFound)

        response = Family("fam_1", "Familia Encontrada")
        shouldDelay = true
        controllableViewModel.loadFamily("fam_1")
        testScheduler.runCurrent()

        assertTrue(controllableViewModel.uiState.value.isLoading)
        assertFalse(controllableViewModel.uiState.value.hasSearched)
        assertFalse(controllableViewModel.uiState.value.isFamilyNotFound)

        advanceUntilIdle()

        val state = controllableViewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasSearched)
        assertFalse(state.isFamilyNotFound)
        assertEquals("fam_1", state.family?.id)
    }

    @Test
    fun testCreateFamilySuccessDoesNotMarkFamilyAsSearched() = runTest {
        viewModel.createFamily("fam_500", "Familia Creada")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCreatedSuccessfully)
        assertFalse(state.hasSearched)
    }
}
