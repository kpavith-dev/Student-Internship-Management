package com.internmatch.app.domain.model

/**
 * Maps a student's profile to a skill in the master catalogue with an assigned proficiency level.
 */
data class SkillProficiency(
    val skillId: String,
    val level: ProficiencyLevel,
)
