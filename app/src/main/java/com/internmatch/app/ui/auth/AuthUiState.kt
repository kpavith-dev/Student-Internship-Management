package com.internmatch.app.ui.auth

import com.internmatch.app.domain.model.User

/**
 * Immutable UI State representing authentication status, active user session,
 * loading indicator state, and actionable user feedback messages.
 */
data class AuthUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val successMessage: String? = null
)
