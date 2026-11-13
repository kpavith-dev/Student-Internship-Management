package com.internmatch.app.data.repository

import com.internmatch.app.domain.model.Skill
import com.internmatch.app.domain.repository.SkillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production integration boundary implementation of [SkillRepository].
 *
 * Jeevithan owns master skill catalogue management and student skill selection per Accepted Decision 2.
 * Production DI instantiates this adapter boundary until Jeevithan's production
 * Firestore-backed skill catalogue repository is registered.
 */
class DefaultSkillRepository : SkillRepository {

    private val _masterSkills = MutableStateFlow<List<Skill>>(emptyList())
    override val masterSkills: Flow<List<Skill>> = _masterSkills.asStateFlow()

    override suspend fun getMasterSkills(): List<Skill> {
        return _masterSkills.value
    }

    override suspend fun getSkillById(skillId: String): Skill? {
        return _masterSkills.value.find { it.id == skillId }
    }
}
