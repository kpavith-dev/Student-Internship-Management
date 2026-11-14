package com.internmatch.app.ui.application

import com.internmatch.app.domain.model.Application
import com.internmatch.app.domain.model.ApplicationStatusHistory

/**
 * UI State for application submission (FR18).
 */
data class ApplicationSubmissionUiState(
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val submittedApplicationId: String? = null,
    val errorMessage: String? = null
)

/**
 * UI State for student application list (FR19).
 */
data class StudentApplicationsUiState(
    val isLoading: Boolean = false,
    val applications: List<Application> = emptyList(),
    val errorMessage: String? = null,
    val isEmpty: Boolean = false
)

/**
 * UI State for application details and withdrawal workflow (FR19, FR21).
 */
data class ApplicationDetailsUiState(
    val isLoading: Boolean = false,
    val application: Application? = null,
    val history: List<ApplicationStatusHistory> = emptyList(),
    val isWithdrawing: Boolean = false,
    val withdrawSuccess: Boolean = false,
    val errorMessage: String? = null
)
