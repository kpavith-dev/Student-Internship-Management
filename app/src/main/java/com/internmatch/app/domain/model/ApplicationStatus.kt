package com.internmatch.app.domain.model

/**
 * Represents the workflow state of an application submitted by a student for an internship.
 */
enum class ApplicationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    SHORTLISTED,
    ACCEPTED,
    REJECTED,
    WITHDRAWN
}
