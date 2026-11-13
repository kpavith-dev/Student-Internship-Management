package com.internmatch.app.domain.service

import com.internmatch.app.domain.model.RequiredSkill
import com.internmatch.app.domain.model.SkillProficiency

/**
 * Implementation of [MatchScoreProvider] applying the accepted pure Kotlin matching formula.
 * Match Score = (Sum of matched required-skill weights / Total required-skill weights) * 100
 * Returns null if required skills list is empty (zero-weight rule).
 * Non-skill factors (GPA, location, proficiency levels) do not affect score.
 */
class DefaultMatchScoreProvider : MatchScoreProvider {

    override fun calculateMatchScore(
        studentSkills: List<SkillProficiency>,
        requiredSkills: List<RequiredSkill>
    ): Double? {
        if (requiredSkills.isEmpty()) return null

        val totalWeight = requiredSkills.sumOf { it.weight }
        if (totalWeight <= 0.0) return null

        val studentSkillIds = studentSkills.map { it.skillId.trim().lowercase() }.toSet()

        val matchedWeight = requiredSkills.filter { required ->
            studentSkillIds.contains(required.skillId.trim().lowercase())
        }.sumOf { it.weight }

        val score = (matchedWeight / totalWeight) * 100.0
        return score
    }
}
