package com.internmatch.app.data.repository

import com.internmatch.app.domain.model.Skill
import com.internmatch.app.domain.repository.SkillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Isolated fake implementation of [SkillRepository] containing fake test catalogue skills
 * exclusively under test sources.
 */
class FakeSkillRepository(
    initialSkills: List<Skill> = DEFAULT_TEST_SKILLS
) : SkillRepository {

    private val _masterSkills = MutableStateFlow(initialSkills)
    override val masterSkills: Flow<List<Skill>> = _masterSkills.asStateFlow()

    override suspend fun getMasterSkills(): List<Skill> {
        return _masterSkills.value
    }

    override suspend fun getSkillById(skillId: String): Skill? {
        return _masterSkills.value.find { it.id == skillId }
    }

    companion object {
        val DEFAULT_TEST_SKILLS = listOf(
            Skill(id = "kotlin", name = "Kotlin"),
            Skill(id = "android", name = "Android SDK"),
            Skill(id = "jetpack-compose", name = "Jetpack Compose"),
            Skill(id = "firebase", name = "Firebase"),
            Skill(id = "java", name = "Java"),
            Skill(id = "python", name = "Python"),
            Skill(id = "sql", name = "SQL & Databases"),
            Skill(id = "git", name = "Git & Version Control"),
            Skill(id = "rest-api", name = "RESTful APIs"),
            Skill(id = "ui-ux", name = "UI/UX Design"),
            Skill(id = "unit-testing", name = "Unit Testing")
        )
    }
}
