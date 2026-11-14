package com.internmatch.app.ui.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.internmatch.app.domain.repository.ApplicationRepository
import com.internmatch.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing student application submission, tracking, details, and withdrawal workflows (FR18, FR19, FR21).
 */
class ApplicationViewModel(
    private val applicationRepository: ApplicationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _submissionState = MutableStateFlow(ApplicationSubmissionUiState())
    val submissionState: StateFlow<ApplicationSubmissionUiState> = _submissionState.asStateFlow()

    private val _applicationsState = MutableStateFlow(StudentApplicationsUiState(isLoading = true))
    val applicationsState: StateFlow<StudentApplicationsUiState> = _applicationsState.asStateFlow()

    private val _detailsState = MutableStateFlow(ApplicationDetailsUiState())
    val detailsState: StateFlow<ApplicationDetailsUiState> = _detailsState.asStateFlow()

    init {
        observeStudentApplications()
    }

    /**
     * Submits an application for the specified internship (FR18).
     */
    fun submitApplication(
        internshipId: String,
        coverLetter: String = "",
        resumeUrl: String = ""
    ) {
        if (_submissionState.value.isSubmitting) return

        _submissionState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = applicationRepository.submitApplication(
                internshipId = internshipId,
                coverLetter = coverLetter,
                resumeUrl = resumeUrl
            )

            if (result.isSuccess) {
                val app = result.getOrNull()
                _submissionState.update {
                    it.copy(
                        isSubmitting = false,
                        isSuccess = true,
                        submittedApplicationId = app?.id,
                        errorMessage = null
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Application submission failed."
                _submissionState.update {
                    it.copy(
                        isSubmitting = false,
                        isSuccess = false,
                        errorMessage = err
                    )
                }
            }
        }
    }

    fun resetSubmissionState() {
        _submissionState.value = ApplicationSubmissionUiState()
    }

    /**
     * Streams applications for the current authenticated student (FR19).
     */
    fun observeStudentApplications() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user == null) {
                    _applicationsState.update {
                        it.copy(isLoading = false, applications = emptyList(), isEmpty = true, errorMessage = null)
                    }
                } else {
                    _applicationsState.update { it.copy(isLoading = true, errorMessage = null) }
                    applicationRepository.getStudentApplications(user.id)
                        .onEach { list ->
                            _applicationsState.update {
                                it.copy(
                                    isLoading = false,
                                    applications = list,
                                    isEmpty = list.isEmpty(),
                                    errorMessage = null
                                )
                            }
                        }
                        .catch { e ->
                            _applicationsState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = e.message ?: "Failed to load applications."
                                )
                            }
                        }
                        .launchIn(viewModelScope)
                }
            }
        }
    }

    /**
     * Loads application details and full chronological status history (FR19).
     */
    fun loadApplicationDetails(applicationId: String) {
        _detailsState.update { it.copy(isLoading = true, errorMessage = null) }

        applicationRepository.getApplicationById(applicationId)
            .onEach { app ->
                _detailsState.update { it.copy(application = app) }
                if (app != null) {
                    loadHistory(app.id)
                } else {
                    _detailsState.update {
                        it.copy(isLoading = false, errorMessage = "Application not found.")
                    }
                }
            }
            .catch { e ->
                _detailsState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load application details.")
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadHistory(applicationId: String) {
        applicationRepository.getApplicationHistory(applicationId)
            .onEach { historyList ->
                _detailsState.update {
                    it.copy(
                        isLoading = false,
                        history = historyList
                    )
                }
            }
            .catch { e ->
                _detailsState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load status history."
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Withdraws the specified active application (FR21).
     */
    fun withdrawApplication(applicationId: String) {
        if (_detailsState.value.isWithdrawing) return

        _detailsState.update { it.copy(isWithdrawing = true, errorMessage = null) }

        viewModelScope.launch {
            val result = applicationRepository.withdrawApplication(applicationId)
            if (result.isSuccess) {
                _detailsState.update {
                    it.copy(
                        isWithdrawing = false,
                        withdrawSuccess = true,
                        errorMessage = null
                    )
                }
                loadApplicationDetails(applicationId)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Application withdrawal failed."
                _detailsState.update {
                    it.copy(
                        isWithdrawing = false,
                        withdrawSuccess = false,
                        errorMessage = err
                    )
                }
            }
        }
    }
}
