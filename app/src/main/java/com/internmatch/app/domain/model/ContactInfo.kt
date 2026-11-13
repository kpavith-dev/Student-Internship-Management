package com.internmatch.app.domain.model

/**
 * Immutable domain model representing student contact details.
 */
data class ContactInfo(
    val phone: String = "",
    val linkedinUrl: String = "",
    val githubUrl: String = "",
    val portfolioUrl: String = ""
)
