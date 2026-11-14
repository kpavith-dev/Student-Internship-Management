package com.internmatch.app.ui.application

import com.internmatch.app.domain.model.Application
import com.internmatch.app.domain.model.ApplicationException
import com.internmatch.app.domain.model.ApplicationStatus
import com.internmatch.app.domain.model.ApplicationStatusHistory
import com.internmatch.app.domain.model.User
import com.internmatch.app.domain.model.UserRole
import com.internmatch.app.domain.repository.ApplicationRepository
import com.internmatch.app.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ApplicationViewModel
    private lateinit var fakeApplicationRepository: FakeApplicationRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeApplicationRepository = FakeApplicationRepository()
        fakeAuthRepository = FakeAuthRepository()

        fakeAuthRepository.setUser(
            User(id = "student_123", email = "student@test.com", displayName = "Test Student", role = UserRole.STUDENT)
        )

        viewModel = ApplicationViewModel(
            applicationRepository = fakeApplicationRepository,
            authRepository = fakeAuthRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial states are correctly initialized`() {
        assertFalse(viewModel.submissionState.value.isSubmitting)
        assertFalse(viewModel.submissionState.value.isSuccess)

        assertFalse(viewModel.detailsState.value.isLoading)
        assertFalse(viewModel.detailsState.value.isWithdrawing)
    }

    @Test
    fun `submitApplication success updates submissionState`() = runTest {
        viewModel.submitApplication("internship_1", coverLetter = "Cover letter text")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.submissionState.value
        assertFalse(state.isSubmitting)
        assertTrue(state.isSuccess)
        assertEquals("internship_1_student_123", state.submittedApplicationId)
    }

    @Test
    fun `submitApplication failure updates submissionState with error message`() = runTest {
        fakeApplicationRepository.shouldFailSubmit = true

        viewModel.submitApplication("internship_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.submissionState.value
        assertFalse(state.isSubmitting)
        assertFalse(state.isSuccess)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `observeStudentApplications loads applications for authenticated user`() = runTest {
        fakeApplicationRepository.addApplication(
            Application(id = "app_1", internshipId = "internship_1", studentId = "student_123", companyName = "Wso2", internshipTitle = "Intern")
        )

        viewModel.observeStudentApplications()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.applicationsState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertEquals(1, state.applications.size)
        assertEquals("Wso2", state.applications[0].companyName)
    }

    @Test
    fun `loadApplicationDetails loads application and status history`() = runTest {
        val app = Application(id = "app_1", internshipId = "internship_1", studentId = "student_123", companyName = "Wso2")
        fakeApplicationRepository.addApplication(app)
        fakeApplicationRepository.addHistory("app_1", ApplicationStatusHistory(id = "h1", applicationId = "app_1", status = ApplicationStatus.SUBMITTED))

        viewModel.loadApplicationDetails("app_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.detailsState.value
        assertFalse(state.isLoading)
        assertNotNull(state.application)
        assertEquals("Wso2", state.application?.companyName)
        assertEquals(1, state.history.size)
    }

    @Test
    fun `withdrawApplication success updates detailsState`() = runTest {
        val app = Application(id = "app_1", internshipId = "internship_1", studentId = "student_123", status = ApplicationStatus.SUBMITTED)
        fakeApplicationRepository.addApplication(app)

        viewModel.withdrawApplication("app_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.detailsState.value
        assertFalse(state.isWithdrawing)
        assertTrue(state.withdrawSuccess)
    }

    private class FakeAuthRepository : AuthRepository {
        private val _currentUser = MutableStateFlow<User?>(null)
        override val currentUser: Flow<User?> = _currentUser

        fun setUser(user: User?) { _currentUser.value = user }
        override suspend fun getCurrentUser(): User? = _currentUser.value
        override suspend fun signIn(email: String, password: String): Result<User> = Result.failure(NotImplementedError())
        override suspend fun signUp(email: String, password: String, displayName: String, role: UserRole): Result<User> = Result.failure(NotImplementedError())
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun signOut(): Result<Unit> { _currentUser.value = null; return Result.success(Unit) }
    }

    private class FakeApplicationRepository : ApplicationRepository {
        var shouldFailSubmit = false
        private val apps = mutableListOf<Application>()
        private val histories = mutableMapOf<String, MutableList<ApplicationStatusHistory>>()

        fun addApplication(app: Application) { apps.add(app) }
        fun addHistory(appId: String, history: ApplicationStatusHistory) {
            histories.getOrPut(appId) { mutableListOf() }.add(history)
        }

        override suspend fun submitApplication(internshipId: String, coverLetter: String, resumeUrl: String): Result<Application> {
            if (shouldFailSubmit) return Result.failure(ApplicationException.AlreadyApplied)
            val app = Application(id = "${internshipId}_student_123", internshipId = internshipId, studentId = "student_123", coverLetter = coverLetter)
            apps.add(app)
            return Result.success(app)
        }

        override suspend fun hasStudentApplied(studentId: String, internshipId: String): Result<Boolean> {
            return Result.success(apps.any { it.internshipId == internshipId && it.studentId == studentId })
        }

        override fun getStudentApplications(studentId: String): Flow<List<Application>> = flowOf(apps.filter { it.studentId == studentId })

        override fun getApplicationById(applicationId: String): Flow<Application?> = flowOf(apps.find { it.id == applicationId })

        override fun getApplicationHistory(applicationId: String): Flow<List<ApplicationStatusHistory>> = flowOf(histories[applicationId] ?: emptyList())

        override suspend fun withdrawApplication(applicationId: String): Result<Unit> {
            val app = apps.find { it.id == applicationId } ?: return Result.failure(ApplicationException.ApplicationNotFound)
            val updated = app.copy(status = ApplicationStatus.WITHDRAWN)
            apps.remove(app)
            apps.add(updated)
            return Result.success(Unit)
        }
    }
}
