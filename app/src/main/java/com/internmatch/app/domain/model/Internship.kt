package com.internmatch.app.domain.model

/**
 * Internship posting model representing an available internship opportunity.
 *
 * PROPOSED FR16 SCHEMA EXTENSIONS:
 * - [domain]: Field of specialization (e.g. "Software Engineering", "Data Science", "UI/UX Design").
 * - [district]: Geographic district filter (e.g. "Colombo", "Gampaha", "Kandy").
 * - [workMode]: Work location model (e.g. "ON_SITE", "HYBRID", "REMOTE").
 *
 * Note: [location] represents a specific street/building address, whereas [district] is a structured
 * filter boundary. [employmentType] represents commitment (e.g. "FULL_TIME"), whereas [workMode] is location model.
 */
data class Internship(
    val id: String = "",
    val providerId: String = "",
    val companyName: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val employmentType: String = "",
    val domain: String = "",
    val district: String = "",
    val workMode: String = "",
    val requiredSkills: List<RequiredSkill> = emptyList(),
    val status: InternshipStatus = InternshipStatus.APPROVED,
    val applicationDeadline: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
