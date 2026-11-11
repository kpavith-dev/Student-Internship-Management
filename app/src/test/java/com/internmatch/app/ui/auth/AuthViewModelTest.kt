package com.internmatch.app.ui.auth

import com.internmatch.app.data.repository.FakeAuthRepository
import com.internmatch.app.domain.model.User
import com.internmatch.app.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isNotLoadingAndNoUser() {
        runTest {
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.currentUser)
            assertNull(state.errorMessage)
            assertFalse(state.isSuccess)
        }
    }

    @Test
    fun signIn_success_updatesUiStateWithUserAndSuccess() {
        runTest {
            val testUser = User("usr_101", "student@example.com", "Jane Student", UserRole.STUDENT)
            fakeAuthRepository.seedUser(testUser, "password123")

            viewModel.signIn("student@example.com", "password123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(testUser, state.currentUser)
            assertTrue(state.isSuccess)
            assertEquals("Sign-in successful.", state.successMessage)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun signIn_failure_updatesUiStateWithError() {
        runTest {
            viewModel.signIn("nonexistent@example.com", "wrongpass")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.currentUser)
            assertFalse(state.isSuccess)
            assertEquals("Invalid email or password. Please check your credentials.", state.errorMessage)
        }
    }

    @Test
    fun signUp_success_updatesUiState() {
        runTest {
            viewModel.signUp("newstudent@example.com", "securePassword123", "New Student", UserRole.STUDENT)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isSuccess)
            assertEquals("Account created successfully.", state.successMessage)
            assertEquals("newstudent@example.com", state.currentUser?.email)
            assertEquals(UserRole.STUDENT, state.currentUser?.role)
        }
    }

    @Test
    fun signUp_failure_weakPassword_updatesUiStateWithError() {
        runTest {
            viewModel.signUp("new@example.com", "123", "Short Pass", UserRole.STUDENT)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isSuccess)
            assertEquals("The password provided is too weak. Please use a stronger password.", state.errorMessage)
        }
    }

    @Test
    fun signUp_adminRole_failsWithError() {
        runTest {
            viewModel.signUp("admin@example.com", "securePassword123", "Admin User", UserRole.ADMIN)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isSuccess)
            assertTrue(state.errorMessage?.contains("ADMIN") == true)
        }
    }

    @Test
    fun sendPasswordReset_success_updatesSuccessMessage() {
        runTest {
            val testUser = User("usr_202", "reset@example.com", "Reset User", UserRole.STUDENT)
            fakeAuthRepository.seedUser(testUser, "pass12345")

            viewModel.sendPasswordReset("reset@example.com")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isSuccess)
            assertEquals("Password reset email sent.", state.successMessage)
        }
    }

    @Test
    fun sendPasswordReset_failure_userNotFound_updatesErrorMessage() {
        runTest {
            viewModel.sendPasswordReset("unknown@example.com")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isSuccess)
            assertEquals("No registered user account was found with this email.", state.errorMessage)
        }
    }

    @Test
    fun signOut_clearsCurrentUser() {
        runTest {
            val testUser = User("usr_303", "active@example.com", "Active User", UserRole.STUDENT)
            fakeAuthRepository.seedUser(testUser, "pass12345")
            viewModel.signIn("active@example.com", "pass12345")
            advanceUntilIdle()

            viewModel.signOut()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.currentUser)
            assertTrue(state.isSuccess)
            assertEquals("Signed out.", state.successMessage)
        }
    }

    @Test
    fun clearError_removesErrorMessage() {
        runTest {
            viewModel.signIn("invalid", "")
            advanceUntilIdle()

            viewModel.clearError()

            assertNull(viewModel.uiState.value.errorMessage)
        }
    }

    @Test
    fun clearSuccess_removesSuccessState() {
        runTest {
            val testUser = User("usr_404", "test@example.com", "Test", UserRole.STUDENT)
            fakeAuthRepository.seedUser(testUser, "password123")
            viewModel.signIn("test@example.com", "password123")
            advanceUntilIdle()

            viewModel.clearSuccess()

            assertFalse(viewModel.uiState.value.isSuccess)
            assertNull(viewModel.uiState.value.successMessage)
        }
    }
}
