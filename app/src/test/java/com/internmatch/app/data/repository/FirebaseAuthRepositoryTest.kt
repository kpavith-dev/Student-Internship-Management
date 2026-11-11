package com.internmatch.app.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.internmatch.app.domain.model.AuthException
import com.internmatch.app.domain.model.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [FirebaseAuthRepository] error mapping using actual Firebase SDK exception instances,
 * [CancellationException] rethrowing, ADMIN role security guard, sign-out session management, and password reset handling.
 *
 * Mock-based unit test suite independent of live Firebase network/credentials.
 */
class FirebaseAuthRepositoryTest {

    private lateinit var repository: FirebaseAuthRepository
    private lateinit var mockAuthClient: MockFirebaseAuthClient

    @Before
    fun setUp() {
        mockAuthClient = MockFirebaseAuthClient()
        repository = FirebaseAuthRepository(firebaseAuthClient = mockAuthClient)
    }

    @Test
    fun mapException_invalidCredentials_returnsInvalidCredentialsAuthException() {
        val ex = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIALS", "Invalid login credentials")
        val domainEx = repository.mapExceptionToDomain(ex)
        assertTrue(domainEx is AuthException.InvalidCredentials)
    }

    @Test
    fun mapException_emailAlreadyInUse_returnsEmailAlreadyInUseAuthException() {
        val ex = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "The email address is already in use")
        val domainEx = repository.mapExceptionToDomain(ex)
        assertTrue(domainEx is AuthException.EmailAlreadyInUse)
    }

    @Test
    fun mapException_weakPassword_returnsWeakPasswordAuthException() {
        val ex = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "Password should be at least 6 characters", null)
        val domainEx = repository.mapExceptionToDomain(ex)
        assertTrue(domainEx is AuthException.WeakPassword)
    }

    @Test
    fun mapException_userNotFound_returnsUserNotFoundAuthException() {
        val ex = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "There is no user record corresponding to this identifier")
        val domainEx = repository.mapExceptionToDomain(ex)
        assertTrue(domainEx is AuthException.UserNotFound)
    }

    @Test
    fun mapException_networkError_returnsNetworkErrorAuthException() {
        val ex = FirebaseNetworkException("A network error occurred")
        val domainEx = repository.mapExceptionToDomain(ex)
        assertTrue(domainEx is AuthException.NetworkError)
    }

    @Test
    fun mapException_tooManyRequests_returnsTooManyRequestsAuthException() {
        val ex = FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "We have blocked all requests from this device due to unusual activity")
        val domainEx = repository.mapExceptionToDomain(ex)
        assertTrue(domainEx is AuthException.TooManyRequests)
    }

    @Test
    fun mapException_uninitializedFirebase_returnsServiceUnavailableAuthException() {
        val ex = IllegalStateException("Default FirebaseApp is not initialized in this process")
        val domainEx = repository.mapExceptionToDomain(ex)
        assertTrue(domainEx is AuthException.ServiceUnavailable)
    }

    @Test(expected = CancellationException::class)
    fun mapException_cancellationException_rethrown() {
        val ex = CancellationException("Coroutines Job was cancelled")
        repository.mapExceptionToDomain(ex)
    }

    @Test
    fun signUp_adminRole_rejectedBySecurityGuard() = runBlocking {
        val result = repository.signUp("admin@internmatch.com", "securePass123", "Admin User", UserRole.ADMIN)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is AuthException.InvalidCredentials)
        assertTrue(ex?.message?.contains("Self-registration as ADMIN is prohibited") == true)
    }

    @Test
    fun sendPasswordResetEmail_success_returnsSuccess() = runBlocking {
        val result = repository.sendPasswordResetEmail("user@test.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun sendPasswordResetEmail_failure_returnsMappedAuthException() = runBlocking {
        mockAuthClient.shouldFailPasswordReset = true
        val result = repository.sendPasswordResetEmail("nonexistent@test.com")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthException.UserNotFound)
    }

    @Test
    fun sendPasswordResetEmail_unconfigured_returnsServiceUnavailable() = runBlocking {
        val unconfiguredRepo = FirebaseAuthRepository(firebaseAuthClient = null)
        val result = unconfiguredRepo.sendPasswordResetEmail("user@test.com")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthException.ServiceUnavailable)
    }

    @Test
    fun signOut_configuredSuccess_clearsSessionAndReturnsSuccess() = runBlocking {
        val signInResult = repository.signIn("student@test.com", "validPass123")
        assertTrue(signInResult.isSuccess)
        assertNotNull(repository.getCurrentUser())

        val result = repository.signOut()
        assertTrue(result.isSuccess)
        assertNull(repository.getCurrentUser())
    }

    @Test
    fun signOut_configuredFailure_preservesSessionAndReturnsFailure() = runBlocking {
        val signInResult = repository.signIn("student@test.com", "validPass123")
        assertTrue(signInResult.isSuccess)
        val activeUser = repository.getCurrentUser()
        assertNotNull(activeUser)

        mockAuthClient.shouldFailSignOut = true
        val result = repository.signOut()

        assertTrue(result.isFailure)
        assertEquals(activeUser, repository.getCurrentUser())
    }

    @Test
    fun signOut_unconfigured_returnsServiceUnavailable() = runBlocking {
        val unconfiguredRepo = FirebaseAuthRepository(firebaseAuthClient = null)
        val result = unconfiguredRepo.signOut()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthException.ServiceUnavailable)
    }

    @Test
    fun unconfiguredState_returnsServiceUnavailable() = runBlocking {
        val unconfiguredRepo = FirebaseAuthRepository(firebaseAuthClient = null)
        val result = unconfiguredRepo.signIn("user@test.com", "pass12345")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthException.ServiceUnavailable)
    }
}

class MockFirebaseAuthClient : FirebaseAuthClient {
    override var isConfigured: Boolean = true
    var shouldFailSignOut: Boolean = false
    var shouldFailPasswordReset: Boolean = false

    override suspend fun signInWithEmailAndPassword(email: String, password: String): TypedAuthResult {
        return TypedAuthResult("uid_123", email, "Mock User")
    }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String): TypedAuthResult {
        return TypedAuthResult("uid_456", email, "New User")
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        if (shouldFailPasswordReset) {
            throw FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "USER_NOT_FOUND")
        }
    }

    override fun signOut() {
        if (shouldFailSignOut) {
            throw RuntimeException("Sign-out network error")
        }
    }
}
