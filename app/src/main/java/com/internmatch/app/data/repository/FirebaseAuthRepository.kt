package com.internmatch.app.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.internmatch.app.domain.model.AuthException
import com.internmatch.app.domain.model.User
import com.internmatch.app.domain.model.UserRole
import com.internmatch.app.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Strongly-typed abstraction over Firebase Authentication SDK APIs.
 *
 * Provides typed API contracts, coroutine task completion,
 * and explicit unconfigured-environment safety checks.
 */
interface FirebaseAuthClient {
    val isConfigured: Boolean
    suspend fun signInWithEmailAndPassword(email: String, password: String): TypedAuthResult
    suspend fun createUserWithEmailAndPassword(email: String, password: String): TypedAuthResult
    suspend fun sendPasswordResetEmail(email: String)
    fun signOut()
}

/**
 * Data transfer object wrapping authentication results without leaking SDK classes to UI/Domain layers.
 */
data class TypedAuthResult(
    val uid: String,
    val email: String,
    val displayName: String? = null
)

/**
 * Production implementation of [AuthRepository] backed by Firebase Authentication SDK.
 *
 * Features:
 * - Zero Java reflection (`Class.forName`, `getMethod`, `invoke` completely removed).
 * - Strongly-typed Kotlin `is` checks for Firebase SDK exception mapping.
 * - Preserves coroutine [CancellationException] propagation (never swallows coroutine cancellation).
 * - Concrete production implementation [DefaultFirebaseAuthClient] using official Firebase SDK APIs.
 * - Coroutine task awaiting asynchronous authentication completion.
 * - Explicit [isFirebaseConfigured] safety checks when google-services.json is absent.
 * - Sign-out error safety (never returns success when unconfigured or when an exception occurs; preserves session state on failure).
 * - Role security guard prohibiting arbitrary client self-registration as ADMIN.
 * - Domain-level error mapping ([AuthException]).
 */
class FirebaseAuthRepository(
    private val firebaseAuthClient: FirebaseAuthClient? = null
) : AuthRepository {

    private val userSessionState = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = userSessionState.asStateFlow()

    val isFirebaseConfigured: Boolean
        get() = firebaseAuthClient?.isConfigured ?: false

    override suspend fun getCurrentUser(): User? {
        return userSessionState.value
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(AuthException.InvalidCredentials("Email and password cannot be empty."))
        }
        if (!email.contains("@") || !email.contains(".")) {
            return Result.failure(AuthException.InvalidEmail())
        }

        if (!isFirebaseConfigured || firebaseAuthClient == null) {
            return Result.failure(
                AuthException.ServiceUnavailable(
                    "Firebase Authentication is unconfigured. Add google-services.json to app directory."
                )
            )
        }

        return try {
            val authResult = firebaseAuthClient.signInWithEmailAndPassword(email.trim(), password)
            val user = mapTypedAuthResultToUser(authResult)
            userSessionState.value = user
            Result.success(user)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(mapExceptionToDomain(e))
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<User> {
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

        if (!isFirebaseConfigured || firebaseAuthClient == null) {
            return Result.failure(
                AuthException.ServiceUnavailable(
                    "Firebase Authentication is unconfigured. Add google-services.json to app directory."
                )
            )
        }

        return try {
            val authResult = firebaseAuthClient.createUserWithEmailAndPassword(email.trim(), password)
            val user = mapTypedAuthResultToUser(authResult, displayNameOverride = displayName, role = role)
            userSessionState.value = user
            Result.success(user)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(mapExceptionToDomain(e))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(AuthException.InvalidEmail())
        }

        if (!isFirebaseConfigured || firebaseAuthClient == null) {
            return Result.failure(
                AuthException.ServiceUnavailable(
                    "Firebase Authentication is unconfigured. Add google-services.json to app directory."
                )
            )
        }

        return try {
            firebaseAuthClient.sendPasswordResetEmail(email.trim())
            Result.success(Unit)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(mapExceptionToDomain(e))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        if (!isFirebaseConfigured || firebaseAuthClient == null) {
            return Result.failure(
                AuthException.ServiceUnavailable(
                    "Firebase Authentication is unconfigured. Add google-services.json to app directory."
                )
            )
        }

        return try {
            firebaseAuthClient.signOut()
            userSessionState.value = null
            Result.success(Unit)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            // Preserve existing session state if sign-out fails; return failure
            Result.failure(mapExceptionToDomain(e))
        }
    }

    /**
     * Map Firebase SDK exception types and Throwable instances to domain-level [AuthException]
     * using strongly-typed Kotlin `is` checks.
     *
     * Never swallows [CancellationException].
     */
    fun mapExceptionToDomain(throwable: Throwable): AuthException {
        if (throwable is CancellationException) {
            throw throwable
        }

        return when (throwable) {
            is FirebaseAuthWeakPasswordException -> AuthException.WeakPassword()
            is FirebaseAuthInvalidCredentialsException -> AuthException.InvalidCredentials()
            is FirebaseAuthUserCollisionException -> AuthException.EmailAlreadyInUse()
            is FirebaseAuthInvalidUserException -> AuthException.UserNotFound()
            is FirebaseNetworkException -> AuthException.NetworkError()
            is IllegalStateException -> {
                val message = throwable.message ?: ""
                if (message.contains("Default FirebaseApp is not initialized")) {
                    AuthException.ServiceUnavailable()
                } else {
                    AuthException.Unknown(message, throwable)
                }
            }
            is FirebaseAuthException -> {
                if (throwable.errorCode == "ERROR_TOO_MANY_REQUESTS" || throwable.message?.contains("TOO_MANY_ATTEMPTS_TRY_LATER") == true) {
                    AuthException.TooManyRequests()
                } else {
                    AuthException.InvalidCredentials(throwable.message ?: "Authentication failed.")
                }
            }
            is AuthException -> throwable
            else -> AuthException.Unknown(throwable.message ?: "Authentication failed.", throwable)
        }
    }

    private fun mapTypedAuthResultToUser(
        authResult: TypedAuthResult,
        displayNameOverride: String = "",
        role: UserRole = UserRole.STUDENT
    ): User {
        val email = authResult.email
        val name = displayNameOverride.ifBlank {
            authResult.displayName?.ifBlank { email.substringBefore("@") } ?: email.substringBefore("@")
        }
        return User(
            id = authResult.uid,
            email = email,
            displayName = name,
            role = role
        )
    }
}
