package com.internmatch.app.domain.service

import com.internmatch.app.domain.model.RequiredSkill
import com.internmatch.app.domain.model.SkillProficiency

/**
 * Domain boundary contract for match score calculation.
 * Encapsulates Jeevithan's [MatchingEngine] calculation service.
 * Returns match score percentage [0.0..100.0] or null if required skills list is empty.
 */
interface MatchScoreProvider {
    /**
     * Calculates the match score percentage between a student's skills and an internship's required skills.
     */
    fun calculateMatchScore(
        studentSkills: List<SkillProficiency>,
        requiredSkills: List<RequiredSkill>
    ): Double?
}
