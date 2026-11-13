package com.internmatch.app.domain.model

/**
 * Internship publication lifecycle status.
 * Per Shared Domain Contract Section 3.4 & Decision 3:
 * Only internships with status = [APPROVED] are visible to students in search and filters.
 */
enum class InternshipStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CLOSED
}
