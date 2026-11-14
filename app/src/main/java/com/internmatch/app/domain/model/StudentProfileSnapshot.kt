package com.internmatch.app.domain.model

/**
 * Immutable profile snapshot captured at the time of internship application submission.
 * Preserves the student's qualification state for application review (FR18).
 */
data class StudentProfileSnapshot(
    val studentId: String = "",
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
) {
    companion object {
        fun fromStudentProfile(profile: StudentProfile): StudentProfileSnapshot {
            return StudentProfileSnapshot(
                studentId = profile.studentId,
                registrationNumber = profile.registrationNumber,
                university = profile.university,
                degreeProgram = profile.degreeProgram,
                academicYear = profile.academicYear,
                semester = profile.semester,
                gpa = profile.gpa,
                bio = profile.bio,
                contactDetails = profile.contactDetails,
                projects = profile.projects,
                certificates = profile.certificates,
                skillProficiencies = profile.skillProficiencies
            )
        }
    }
}
