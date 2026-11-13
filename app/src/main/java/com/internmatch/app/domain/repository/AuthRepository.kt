package com.internmatch.app.domain.repository

import com.internmatch.app.domain.model.User
import com.internmatch.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing user authentication lifecycle,
 * account registration, sign-in sessions, password recovery, and sign-out.
 */
interface AuthRepository {
    /**
     * Flow observing the currently authenticated domain [User], or null if unauthenticated.
     */
    val currentUser: Flow<User?>

    /**
     * Retrieves the current authenticated user instance asynchronously.
     */
    suspend fun getCurrentUser(): User?

    /**
     * Authenticates a user using email and password credentials.
     */
    suspend fun signIn(email: String, password: String): Result<User>

    /**
     * Registers a new user account with email, password, display name, and assigned role.
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<User>

    /**
     * Sends a password reset email to the specified email address.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Signs out the currently authenticated user.
     */
    suspend fun signOut(): Result<Unit>
}
