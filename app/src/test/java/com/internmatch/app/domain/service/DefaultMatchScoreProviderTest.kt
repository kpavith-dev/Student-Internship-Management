package com.internmatch.app.domain.service

import com.internmatch.app.domain.model.ProficiencyLevel
import com.internmatch.app.domain.model.RequiredSkill
import com.internmatch.app.domain.model.SkillProficiency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultMatchScoreProviderTest {

    private val matchScoreProvider = DefaultMatchScoreProvider()

    @Test
    fun `calculateMatchScore returns 100 percent when all required skills match`() {
        val studentSkills = listOf(
            SkillProficiency("kotlin", ProficiencyLevel.BEGINNER),
            SkillProficiency("compose", ProficiencyLevel.ADVANCED)
        )
        val requiredSkills = listOf(
            RequiredSkill("kotlin", 1.0),
            RequiredSkill("compose", 1.0)
        )

        val score = matchScoreProvider.calculateMatchScore(studentSkills, requiredSkills)
        assertEquals(100.0, score!!, 0.001)
    }

    @Test
    fun `calculateMatchScore returns partial percentage equal-weights`() {
        val studentSkills = listOf(
            SkillProficiency("kotlin", ProficiencyLevel.EXPERT)
        )
        val requiredSkills = listOf(
            RequiredSkill("kotlin", 1.0),
            RequiredSkill("python", 1.0)
        )

        val score = matchScoreProvider.calculateMatchScore(studentSkills, requiredSkills)
        assertEquals(50.0, score!!, 0.001)
    }

    @Test
    fun `calculateMatchScore returns 0 percent when no required skills match`() {
        val studentSkills = listOf(
            SkillProficiency("java", ProficiencyLevel.EXPERT)
        )
        val requiredSkills = listOf(
            RequiredSkill("kotlin", 1.0),
            RequiredSkill("compose", 1.0)
        )

        val score = matchScoreProvider.calculateMatchScore(studentSkills, requiredSkills)
        assertEquals(0.0, score!!, 0.001)
    }

    @Test
    fun `calculateMatchScore returns null when required skills list is empty`() {
        val studentSkills = listOf(
            SkillProficiency("kotlin", ProficiencyLevel.EXPERT)
        )
        val requiredSkills = emptyList<RequiredSkill>()

        val score = matchScoreProvider.calculateMatchScore(studentSkills, requiredSkills)
        assertNull(score)
    }
}
