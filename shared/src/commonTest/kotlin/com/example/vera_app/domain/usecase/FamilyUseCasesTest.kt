package com.example.vera_app.domain.usecase

import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.model.FamilyAlreadyExistsException
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.family.GetFamilyUseCase
import com.example.vera_app.repository.FakeFamilyRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FamilyUseCasesTest {

    private lateinit var repository: FakeFamilyRepository
    private lateinit var createFamilyUseCase: CreateFamilyUseCase
    private lateinit var getFamilyUseCase: GetFamilyUseCase

    @BeforeTest
    fun setUp() {
        repository = FakeFamilyRepository()
        createFamilyUseCase = CreateFamilyUseCase(repository)
        getFamilyUseCase = GetFamilyUseCase(repository)
    }

    @Test
    fun testCreateFamilyWithNewIdSuccessfully() = runTest {
        val family = createFamilyUseCase("fam_1", "Familia Pérez")

        assertEquals("fam_1", family.id)
        assertEquals("Familia Pérez", family.name)
        assertEquals(1, repository.createFamilyCallCount)

        val retrieved = repository.getFamilyById("fam_1")
        assertNotNull(retrieved)
        assertEquals("Familia Pérez", retrieved.name)
    }

    @Test
    fun testCreateFamilyRejectsDuplicateIdAndDoesNotOverwriteOriginal() = runTest {
        // 1. Crear la familia original
        createFamilyUseCase("fam_1", "Familia Original")

        // 2. Intentar crear una nueva familia con el mismo ID pero diferente nombre
        assertFailsWith<FamilyAlreadyExistsException> {
            createFamilyUseCase("fam_1", "Familia Intruseada")
        }

        // 3. Comprobar que no se haya incrementado el contador de creación
        assertEquals(1, repository.createFamilyCallCount)

        // 4. Comprobar que la familia original conserve su nombre y datos originales
        val saved = repository.getFamilyById("fam_1")
        assertNotNull(saved)
        assertEquals("Familia Original", saved.name)
    }

    @Test
    fun testDirectRepositoryCreateFamilyRejectsDuplicateId() = runTest {
        val originalFamily = Family("fam_1", "Familia Original")
        repository.createFamily(originalFamily)

        val duplicateFamily = Family("fam_1", "Familia Duplicada Directa")
        assertFailsWith<FamilyAlreadyExistsException> {
            repository.createFamily(duplicateFamily)
        }

        assertEquals(1, repository.createFamilyCallCount)
        assertEquals("Familia Original", repository.getFamilyById("fam_1")?.name)
    }

    @Test
    fun testCreateFamilyRejectsBlankIdOrName() = runTest {
        assertFailsWith<IllegalArgumentException> {
            createFamilyUseCase("", "Familia Pérez")
        }

        assertFailsWith<IllegalArgumentException> {
            createFamilyUseCase("fam_2", "  ")
        }

        assertEquals(0, repository.createFamilyCallCount)
    }

    @Test
    fun testGetFamilyByIdReturnsFamilyWhenExists() = runTest {
        createFamilyUseCase("fam_1", "Familia Pérez")

        val result = getFamilyUseCase("fam_1")
        assertNotNull(result)
        assertEquals("fam_1", result.id)
    }

    @Test
    fun testGetFamilyByIdReturnsNullWhenNotFound() = runTest {
        val result = getFamilyUseCase("fam_non_existent")
        assertNull(result)
    }
}
