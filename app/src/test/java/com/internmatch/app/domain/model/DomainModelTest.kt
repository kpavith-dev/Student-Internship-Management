package com.internmatch.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainModelTest {

    @Test
    fun userRole_containsAllAcceptedRoles() {
        val roles = UserRole.entries.map { it.name }
        assertEquals(3, roles.size)
        assertEquals(listOf("STUDENT", "PROVIDER", "ADMIN"), roles)
    }

    @Test
    fun user_instantiationAndProperties() {
        val user = User(
            id = "user_123",
            email = "test@student.edu",
            displayName = "Jane Doe",
            role = UserRole.STUDENT,
        )

        assertEquals("user_123", user.id)
        assertEquals("test@student.edu", user.email)
        assertEquals("Jane Doe", user.displayName)
        assertEquals(UserRole.STUDENT, user.role)
    }

    @Test
    fun proficiencyLevel_containsAllAcceptedLevels() {
        val levels = ProficiencyLevel.entries.map { it.name }
        assertEquals(4, levels.size)
        assertEquals(listOf("BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"), levels)
    }

    @Test
    fun skill_instantiationAndProperties() {
        val skill = Skill(id = "skill_kt", name = "Kotlin")

        assertEquals("skill_kt", skill.id)
        assertEquals("Kotlin", skill.name)
    }

    @Test
    fun skillProficiency_instantiationAndMapping() {
        val proficiency = SkillProficiency(
            skillId = "skill_kt",
            level = ProficiencyLevel.ADVANCED,
        )

        assertEquals("skill_kt", proficiency.skillId)
        assertEquals(ProficiencyLevel.ADVANCED, proficiency.level)
    }

    @Test
    fun applicationStatus_containsAllAcceptedStatuses() {
        val statuses = ApplicationStatus.entries.map { it.name }
        assertEquals(6, statuses.size)
        assertEquals(
            listOf("SUBMITTED", "UNDER_REVIEW", "SHORTLISTED", "ACCEPTED", "REJECTED", "WITHDRAWN"),
            statuses,
        )
    }
}
