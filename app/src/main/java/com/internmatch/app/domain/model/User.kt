package com.internmatch.app.domain.model

/**
 * Represents generic authenticated account credentials and system role.
 *
 * Authentication details are kept separate from role-specific profile data.
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val role: UserRole,
)
