package com.internmatch.app.domain.repository

import com.internmatch.app.domain.model.Skill
import kotlinx.coroutines.flow.Flow

/**
 * Domain integration repository contract for querying canonical skills
 * from the master catalogue managed by Administrators.
 *
 * Serves as the integration boundary for student skill selection (Accepted Decision 2).
 */
interface SkillRepository {
    /**
     * Flow observing available master catalogue skills.
     */
    val masterSkills: Flow<List<Skill>>

    /**
     * Retrieves the current list of canonical skills from the master catalogue.
     */
    suspend fun getMasterSkills(): List<Skill>

    /**
     * Looks up a skill by its canonical ID.
     */
    suspend fun getSkillById(skillId: String): Skill?
}
