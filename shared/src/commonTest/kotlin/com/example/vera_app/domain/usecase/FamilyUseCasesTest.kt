package com.example.vera_app.domain.usecase

import com.example.vera_app.domain.model.FamilyAlreadyExistsException
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.family.GetFamilyUseCase
import com.example.vera_app.repository.FakeFamilyRepository
import kotlinx.coroutines.runBlocking
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
    fun testCreateFamilySuccessfully() {
        runBlocking {
            val family = createFamilyUseCase("fam_1", "Familia Pérez")

            assertEquals("fam_1", family.id)
            assertEquals("Familia Pérez", family.name)
            assertEquals(1, repository.createFamilyCallCount)

            val retrieved = repository.getFamilyById("fam_1")
            assertNotNull(retrieved)
            assertEquals("Familia Pérez", retrieved.name)
        }
    }

    @Test
    fun testCreateFamilyRejectsDuplicateId() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")

            assertFailsWith<FamilyAlreadyExistsException> {
                createFamilyUseCase("fam_1", "Familia Pérez Duplicada")
            }

            assertEquals(1, repository.createFamilyCallCount)
        }
    }

    @Test
    fun testCreateFamilyRejectsBlankIdOrName() {
        runBlocking {
            assertFailsWith<IllegalArgumentException> {
                createFamilyUseCase("", "Familia Pérez")
            }

            assertFailsWith<IllegalArgumentException> {
                createFamilyUseCase("fam_2", "  ")
            }

            assertEquals(0, repository.createFamilyCallCount)
        }
    }

    @Test
    fun testGetFamilyByIdReturnsFamilyWhenExists() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")

            val result = getFamilyUseCase("fam_1")
            assertNotNull(result)
            assertEquals("fam_1", result.id)
        }
    }

    @Test
    fun testGetFamilyByIdReturnsNullWhenNotFound() {
        runBlocking {
            val result = getFamilyUseCase("fam_non_existent")
            assertNull(result)
        }
    }
}
