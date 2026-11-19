package com.example.vera_app.domain.usecase

import com.example.vera_app.domain.model.FamilyNotFoundException
import com.example.vera_app.domain.model.Member
import com.example.vera_app.domain.model.MemberRole
import com.example.vera_app.domain.usecase.family.CreateFamilyUseCase
import com.example.vera_app.domain.usecase.member.AddMemberUseCase
import com.example.vera_app.repository.FakeFamilyRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class MemberUseCasesTest {

    private lateinit var repository: FakeFamilyRepository
    private lateinit var createFamilyUseCase: CreateFamilyUseCase
    private lateinit var addMemberUseCase: AddMemberUseCase

    @BeforeTest
    fun setUp() {
        repository = FakeFamilyRepository()
        createFamilyUseCase = CreateFamilyUseCase(repository)
        addMemberUseCase = AddMemberUseCase(repository)
    }

    @Test
    fun testAddMemberSuccessfully() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")

            val newMember = Member(id = "mem_1", name = "Carlos", role = MemberRole.PARENT)
            val updatedFamily = addMemberUseCase("fam_1", newMember)

            assertEquals(1, updatedFamily.members.size)
            assertEquals("Carlos", updatedFamily.members.first().name)
            assertEquals(1, repository.saveFamilyCallCount)

            val savedFamily = repository.getFamilyById("fam_1")
            assertNotNull(savedFamily)
            assertEquals(1, savedFamily.members.size)
            assertEquals("Carlos", savedFamily.getMember("mem_1")?.name)
        }
    }

    @Test
    fun testAddMemberRejectsDuplicateMemberId() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")

            val member1 = Member(id = "mem_1", name = "Carlos", role = MemberRole.PARENT)
            addMemberUseCase("fam_1", member1)

            val duplicateMember = Member(id = "mem_1", name = "Carlos Duplicado", role = MemberRole.CHILD)
            assertFailsWith<IllegalArgumentException> {
                addMemberUseCase("fam_1", duplicateMember)
            }

            assertEquals(1, repository.saveFamilyCallCount)

            val savedFamily = repository.getFamilyById("fam_1")
            assertNotNull(savedFamily)
            assertEquals(1, savedFamily.members.size)
            assertEquals("Carlos", savedFamily.members.first().name)
        }
    }

    @Test
    fun testAddMemberThrowsWhenFamilyNotFound() {
        runBlocking {
            val member = Member(id = "mem_1", name = "Carlos")

            assertFailsWith<FamilyNotFoundException> {
                addMemberUseCase("fam_ghost", member)
            }

            assertEquals(0, repository.saveFamilyCallCount)
        }
    }

    @Test
    fun testAddMemberRejectsBlankMemberData() {
        runBlocking {
            createFamilyUseCase("fam_1", "Familia Pérez")

            assertFailsWith<IllegalArgumentException> {
                Member(id = "", name = "Carlos")
            }

            assertFailsWith<IllegalArgumentException> {
                Member(id = "mem_1", name = "  ")
            }

            assertEquals(0, repository.saveFamilyCallCount)
        }
    }
}
