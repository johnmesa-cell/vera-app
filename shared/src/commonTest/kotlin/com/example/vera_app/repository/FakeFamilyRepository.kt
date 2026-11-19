package com.example.vera_app.repository

import com.example.vera_app.domain.model.Family
import com.example.vera_app.domain.repository.FamilyRepository

class FakeFamilyRepository : FamilyRepository {
    private val families = mutableMapOf<String, Family>()

    var saveFamilyCallCount = 0
        private set

    var createFamilyCallCount = 0
        private set

    override suspend fun getFamilyById(id: String): Family? {
        return families[id]
    }

    override suspend fun createFamily(family: Family) {
        createFamilyCallCount++
        families[family.id] = family
    }

    override suspend fun saveFamily(family: Family) {
        saveFamilyCallCount++
        families[family.id] = family
    }

    fun clear() {
        families.clear()
        saveFamilyCallCount = 0
        createFamilyCallCount = 0
    }
}
