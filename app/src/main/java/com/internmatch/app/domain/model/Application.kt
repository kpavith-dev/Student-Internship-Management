package com.internmatch.app.domain.model

/**
 * Domain model representing a student application for an internship (FR18, FR19, FR21).
 */
data class Application(
    val id: String = "",
    val internshipId: String = "",
    val studentId: String = "",
    val companyName: String = "",
    val internshipTitle: String = "",
    val status: ApplicationStatus = ApplicationStatus.SUBMITTED,
    val submittedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val matchScore: Double? = null,
    val profileSnapshot: StudentProfileSnapshot? = null,
    val coverLetter: String = "",
    val resumeUrl: String = ""
) {
    /**
     * Determines whether the application is eligible for student withdrawal (FR21).
     * Students may withdraw prior to a final decision (ACCEPTED or REJECTED).
     */
    val isWithdrawable: Boolean
        get() = status in listOf(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.SHORTLISTED
        )
}
