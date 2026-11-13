package com.internmatch.app.domain.model

/**
 * Immutable domain model representing a student academic or professional certification.
 */
data class Certificate(
    val title: String,
    val issuer: String,
    val issueDate: String = ""
)
