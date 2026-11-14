package com.internmatch.app.domain.model

/**
 * Represents an audit trail event tracking application status transitions (FR19, FR21).
 */
data class ApplicationStatusHistory(
    val id: String = "",
    val applicationId: String = "",
    val status: ApplicationStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val actorUid: String = "",
    val note: String? = null
)
