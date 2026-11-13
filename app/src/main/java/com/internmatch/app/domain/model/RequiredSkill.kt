package com.internmatch.app.domain.model

/**
 * Required skill entry associated with an internship listing.
 * References a canonical [skillId] from the master catalogue.
 * Weight defaults to equal weight (1.0) per accepted equal-weight formula rule.
 */
data class RequiredSkill(
    val skillId: String,
    val weight: Double = 1.0
)
