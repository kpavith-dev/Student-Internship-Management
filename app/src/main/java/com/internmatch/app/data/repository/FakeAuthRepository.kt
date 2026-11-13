package com.internmatch.app.data.repository

import com.internmatch.app.domain.model.AuthException
import com.internmatch.app.domain.model.User
import com.internmatch.app.domain.model.UserRole
import com.internmatch.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory Fake AuthRepository implementation for deterministic unit testing
 * and local offline development.
 */
class FakeAuthRepository : AuthRepository {

    private val userSessionState = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = userSessionState.asStateFlow()

    private val registeredUsers = mutableMapOf<String, Pair<String, User>>()

    // Test control flags for simulating failures in unit tests
    var shouldFailNextOperation: Boolean = false
    var failureExceptionToThrow: AuthException = AuthException.InvalidCredentials()

    override suspend fun getCurrentUser(): User? {
        return userSessionState.value
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        if (shouldFailNextOperation) {
            shouldFailNextOperation = false
            return Result.failure(failureExceptionToThrow)
        }
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(AuthException.InvalidEmail())
        }
        if (password.isBlank()) {
            return Result.failure(AuthException.InvalidCredentials())
        }

        val record = registeredUsers.values.firstOrNull { (p, user) ->
            user.email.equals(email, ignoreCase = true) && p == password
        }

        return if (record != null) {
            val user = record.second
            userSessionState.value = user
            Result.success(user)
        } else {
            Result.failure(AuthException.InvalidCredentials())
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<User> {
        if (shouldFailNextOperation) {
            shouldFailNextOperation = false
            return Result.failure(failureExceptionToThrow)
        }
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(AuthException.InvalidEmail())
        }
        if (password.length < 6) {
            return Result.failure(AuthException.WeakPassword())
        }
        if (displayName.isBlank()) {
            return Result.failure(AuthException.InvalidCredentials("Display name cannot be blank."))
        }

        // ROLE SECURITY GUARD: Self-registration as ADMIN is strictly prohibited.
        if (role == UserRole.ADMIN) {
            return Result.failure(
                AuthException.InvalidCredentials(
                    "Self-registration as ADMIN is prohibited. Admin accounts require administrator provisioning."
                )
            )
        }

        val existing = registeredUsers.values.firstOrNull { (_, user) ->
            user.email.equals(email, ignoreCase = true)
        }
        if (existing != null) {
            return Result.failure(AuthException.EmailAlreadyInUse())
        }

        val userId = "user_${registeredUsers.size + 1}"
        val user = User(
            id = userId,
            email = email,
            displayName = displayName,
            role = role
        )
        registeredUsers[userId] = Pair(password, user)
        userSessionState.value = user
        return Result.success(user)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        if (shouldFailNextOperation) {
            shouldFailNextOperation = false
            return Result.failure(failureExceptionToThrow)
        }
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(AuthException.InvalidEmail())
        }

        val exists = registeredUsers.values.any { (_, user) ->
            user.email.equals(email, ignoreCase = true)
        }
        return if (exists) {
            Result.success(Unit)
        } else {
            Result.failure(AuthException.UserNotFound())
        }
    }

    override suspend fun signOut(): Result<Unit> {
        userSessionState.value = null
        return Result.success(Unit)
    }

    /**
     * Helper method for seeding user credentials in unit tests.
     */
    fun seedUser(user: User, password: String) {
        registeredUsers[user.id] = Pair(password, user)
    }
}
