package com.internmatch.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.internmatch.app.domain.model.UserRole
import com.internmatch.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing authentication workflow state, credential submission,
 * account registration, password reset recovery, and session sign-out.
 *
 * Depends strictly on [AuthRepository] interface following MVVM principles.
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                isSuccess = false,
                successMessage = null
            )
        }

        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            isSuccess = true,
                            successMessage = "Sign-in successful."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Authentication failed."
                        )
                    }
                }
            )
        }
    }

    fun signUp(
        email: String,
        password: String,
        displayName: String,
        role: UserRole = UserRole.STUDENT
    ) {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                isSuccess = false,
                successMessage = null
            )
        }

        viewModelScope.launch {
            val result = authRepository.signUp(email, password, displayName, role)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            isSuccess = true,
                            successMessage = "Account created successfully."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Registration failed."
                        )
                    }
                }
            )
        }
    }

    fun sendPasswordReset(email: String) {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                isSuccess = false,
                successMessage = null
            )
        }

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            successMessage = "Password reset email sent."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to send reset email."
                        )
                    }
                }
            )
        }
    }

    fun signOut() {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update {
                AuthUiState(
                    currentUser = null,
                    isLoading = false,
                    isSuccess = true,
                    successMessage = "Signed out."
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(isSuccess = false, successMessage = null) }
    }
}
