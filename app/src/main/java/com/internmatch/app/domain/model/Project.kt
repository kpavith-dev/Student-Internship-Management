package com.internmatch.app.domain.model

/**
 * Immutable domain model representing a student portfolio project.
 */
data class Project(
    val title: String,
    val description: String,
    val projectUrl: String = ""
)
