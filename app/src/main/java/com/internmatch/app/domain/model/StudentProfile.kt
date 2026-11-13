package com.internmatch.app.domain.model

/**
 * Dedicated domain model representing student profile information.
 *
 * Kept strictly separate from [User] account credentials per Accepted Decision 1.
 * Associated with the authenticated user's UID via [studentId].
 */
data class StudentProfile(
    val studentId: String,
    val registrationNumber: String = "",
    val university: String = "",
    val degreeProgram: String = "",
    val academicYear: Int = 1,
    val semester: Int = 1,
    val gpa: Double? = null,
    val bio: String = "",
    val contactDetails: ContactInfo? = null,
    val projects: List<Project> = emptyList(),
    val certificates: List<Certificate> = emptyList(),
    val skillProficiencies: List<SkillProficiency> = emptyList()
)
