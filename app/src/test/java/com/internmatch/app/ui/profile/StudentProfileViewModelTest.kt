package com.internmatch.app.ui.profile

import com.internmatch.app.data.repository.FakeFirebaseStudentProfileClient
import com.internmatch.app.data.repository.FakeSkillRepository
import com.internmatch.app.data.repository.FirestoreStudentProfileRepository
import com.internmatch.app.domain.model.AuthException
import com.internmatch.app.domain.model.ProficiencyLevel
import com.internmatch.app.domain.model.SkillProficiency
import com.internmatch.app.domain.model.StudentProfile
import com.internmatch.app.domain.model.User
import com.internmatch.app.domain.model.UserRole
import com.internmatch.app.domain.repository.AuthRepository
import com.internmatch.app.domain.repository.StudentProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAuthRepository(
    initialUser: User? = null
) : AuthRepository {
    private val _currentUser = MutableStateFlow(initialUser)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    val activeUser: User?
        get() = _currentUser.value

    fun setUser(user: User?) {
        _currentUser.value = user
    }

    override suspend fun getCurrentUser(): User? = _currentUser.value
    override suspend fun signIn(email: String, password: String): Result<User> = Result.failure(AuthException.ServiceUnavailable())
    override suspend fun signUp(email: String, password: String, displayName: String, role: UserRole): Result<User> = Result.failure(AuthException.ServiceUnavailable())
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.failure(AuthException.ServiceUnavailable())
    override suspend fun signOut(): Result<Unit> {
        _currentUser.value = null
        return Result.success(Unit)
    }
}

class ControllableStudentProfileRepository : StudentProfileRepository {
    private val _currentStudentProfile = MutableStateFlow<StudentProfile?>(null)
    override val currentStudentProfile: Flow<StudentProfile?> = _currentStudentProfile.asStateFlow()

    val deferredMap = mutableMapOf<String, CompletableDeferred<Result<StudentProfile?>>>()

    override suspend fun getStudentProfile(studentId: String): Result<StudentProfile?> {
        val deferred = deferredMap.getOrPut(studentId) { CompletableDeferred() }
        val result = deferred.await()
        if (result.isSuccess) {
            _currentStudentProfile.value = result.getOrNull()
        }
        return result
    }

    override suspend fun createStudentProfile(profile: StudentProfile): Result<StudentProfile> {
        _currentStudentProfile.value = profile
        return Result.success(profile)
    }

    override suspend fun updateStudentProfile(profile: StudentProfile): Result<StudentProfile> {
        _currentStudentProfile.value = profile
        return Result.success(profile)
    }

    override suspend fun updateSkillProficiencies(
        studentId: String,
        skillProficiencies: List<SkillProficiency>
    ): Result<List<SkillProficiency>> {
        val updated = (_currentStudentProfile.value ?: StudentProfile(studentId = studentId))
            .copy(skillProficiencies = skillProficiencies)
        _currentStudentProfile.value = updated
        return Result.success(skillProficiencies)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StudentProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeClient: FakeFirebaseStudentProfileClient
    private lateinit var profileRepository: FirestoreStudentProfileRepository
    private lateinit var skillRepository: FakeSkillRepository
    private lateinit var viewModel: StudentProfileViewModel

    private val testStudentA = User(
        id = "test_student_A",
        email = "studentA@university.ac.lk",
        displayName = "Student A",
        role = UserRole.STUDENT
    )

    private val testStudentB = User(
        id = "test_student_B",
        email = "studentB@university.ac.lk",
        displayName = "Student B",
        role = UserRole.STUDENT
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository(testStudentA)
        fakeClient = FakeFirebaseStudentProfileClient()
        profileRepository = FirestoreStudentProfileRepository(
            profileClient = fakeClient,
            authenticatedUserIdProvider = { fakeAuthRepository.activeUser?.id }
        )
        skillRepository = FakeSkillRepository()

        viewModel = StudentProfileViewModel(
            profileRepository = profileRepository,
            authRepository = fakeAuthRepository,
            skillRepository = skillRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsAvailableSkillsAndObservesUser() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(testStudentA, state.currentUser)
        assertTrue(state.availableSkills.isNotEmpty())
        assertTrue(state.isCreatingProfile)
    }

    @Test
    fun saveProfile_newProfile_createsProfileSuccessfully() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile(
            registrationNumber = "EG/2021/4501",
            university = "University of Jaffna",
            degreeProgram = "Computer Engineering",
            academicYear = 3,
            semester = 2,
            gpa = 3.75,
            bio = "Mobile Developer",
            phone = "+94771112233",
            linkedinUrl = "https://linkedin.com",
            githubUrl = "https://github.com",
            portfolioUrl = "https://pavi.dev"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.profile)
        assertEquals("EG/2021/4501", state.profile?.registrationNumber)
        assertEquals("University of Jaffna", state.profile?.university)
        assertEquals("Computer Engineering", state.profile?.degreeProgram)
        assertEquals(3.75, state.profile?.gpa ?: 0.0, 0.001)
        assertFalse(state.isEditing)
        assertFalse(state.isCreatingProfile)
        assertEquals("Profile saved successfully.", state.successMessage)
    }

    @Test
    fun cancellation_cancelsInFlightJobOnUserSwitch() = runTest {
        val controllableRepo = ControllableStudentProfileRepository()
        val controllableVm = StudentProfileViewModel(
            profileRepository = controllableRepo,
            authRepository = fakeAuthRepository,
            skillRepository = skillRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // User A starts loading profile (controllableRepo holds A's deferred response)
        controllableVm.loadProfile(testStudentA.id)
        val initialJob = controllableVm.activeProfileLoadJob
        assertNotNull(initialJob)

        // User B signs in before A's request completes
        fakeAuthRepository.setUser(testStudentB)
        testDispatcher.scheduler.advanceUntilIdle()

        // Prove User A's initial load job was cancelled by user switch
        assertTrue(initialJob?.isCancelled == true)
    }

    /**
     * Directly tests the internal response-application guard [StudentProfileViewModel.applyProfileLoadResult]
     * by supplying a stale result for User A after User B has become the current active user.
     * Note: This directly verifies the response-application guard method independently of coroutine cancellation.
     */
    @Test
    fun staleResponseGuard_rejectsUserAResponseIfDeliveredAfterUserBSignsIn() = runTest {
        val controllableRepo = ControllableStudentProfileRepository()
        val controllableVm = StudentProfileViewModel(
            profileRepository = controllableRepo,
            authRepository = fakeAuthRepository,
            skillRepository = skillRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // User B signs in and loads profile B
        fakeAuthRepository.setUser(testStudentB)
        controllableRepo.deferredMap[testStudentB.id] = CompletableDeferred(
            Result.success(
                StudentProfile(studentId = testStudentB.id, university = "Uni B", degreeProgram = "CS B")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Uni B", controllableVm.uiState.value.profile?.university)
        assertEquals(testStudentB, controllableVm.uiState.value.currentUser)

        // Directly supply User A's stale result to the response-application guard
        val delayedResultA = Result.success(
            StudentProfile(studentId = testStudentA.id, university = "Uni A", degreeProgram = "CS A")
        )
        controllableVm.applyProfileLoadResult(testStudentA.id, delayedResultA)
        testDispatcher.scheduler.advanceUntilIdle()

        // Prove that the explicit UID guard rejected User A's stale result and preserved User B's state
        assertEquals("Uni B", controllableVm.uiState.value.profile?.university)
        assertEquals(testStudentB, controllableVm.uiState.value.currentUser)
    }

    /**
     * End-to-end repository-level callback test: User A triggers loadProfile, User B signs in mid-flight,
     * and User A's repository deferred completes late. Proves that late repository callbacks for past users
     * are safely filtered out by the UID guard.
     */
    @Test
    fun loadProfile_staleAsyncRepositoryCallback_isIgnoredByUidGuardWhenUserSwitches() = runTest {
        val controllableRepo = ControllableStudentProfileRepository()
        val controllableVm = StudentProfileViewModel(
            profileRepository = controllableRepo,
            authRepository = fakeAuthRepository,
            skillRepository = skillRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. User A initiates loadProfile
        val deferredA = CompletableDeferred<Result<StudentProfile?>>()
        controllableRepo.deferredMap[testStudentA.id] = deferredA
        controllableVm.loadProfile(testStudentA.id)

        // 2. User B signs in before deferredA completes
        fakeAuthRepository.setUser(testStudentB)
        controllableRepo.deferredMap[testStudentB.id] = CompletableDeferred(
            Result.success(
                StudentProfile(studentId = testStudentB.id, university = "Uni B", degreeProgram = "CS B")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Uni B", controllableVm.uiState.value.profile?.university)
        assertEquals(testStudentB, controllableVm.uiState.value.currentUser)

        // 3. User A's late repository callback completes
        deferredA.complete(
            Result.success(
                StudentProfile(studentId = testStudentA.id, university = "Uni A", degreeProgram = "CS A")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 4. Verify User B's state is preserved and User A's stale result was ignored
        assertEquals("Uni B", controllableVm.uiState.value.profile?.university)
        assertEquals(testStudentB, controllableVm.uiState.value.currentUser)
    }

    @Test
    fun userSignOut_resetsCachedProfileStateImmediately() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile(
            registrationNumber = "REG/A",
            university = "Uni A",
            degreeProgram = "CS A",
            academicYear = 3,
            semester = 1,
            gpa = 3.9,
            bio = "Bio A"
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.profile)

        // Simulate user sign out
        fakeAuthRepository.setUser(null)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.currentUser)
        assertNull(state.profile)
        assertFalse(state.isEditing)
        assertFalse(state.isCreatingProfile)
    }

    @Test
    fun saveProfile_validationFailure_setsValidationErrors() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile(
            registrationNumber = "",
            university = "",
            degreeProgram = "",
            academicYear = 0,
            semester = 15,
            gpa = 5.0,
            bio = "Bio"
        )

        val state = viewModel.uiState.value
        assertTrue(state.validationErrors.containsKey("registrationNumber"))
        assertTrue(state.validationErrors.containsKey("university"))
        assertTrue(state.validationErrors.containsKey("degreeProgram"))
        assertTrue(state.validationErrors.containsKey("academicYear"))
        assertTrue(state.validationErrors.containsKey("semester"))
        assertTrue(state.validationErrors.containsKey("gpa"))
        assertNotNull(state.errorMessage)
    }

    @Test
    fun addSkillProficiency_validSkill_addsSkillToProfile() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile(
            registrationNumber = "REG/A",
            university = "Uni A",
            degreeProgram = "CS A",
            academicYear = 3,
            semester = 1,
            gpa = 3.9,
            bio = "Bio A"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addSkillProficiency("kotlin", ProficiencyLevel.ADVANCED)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val skills = state.profile?.skillProficiencies ?: emptyList()
        assertEquals(1, skills.size)
        assertEquals("kotlin", skills.first().skillId)
        assertEquals(ProficiencyLevel.ADVANCED, skills.first().level)
    }

    @Test
    fun addSkillProficiency_duplicateSkill_showsErrorMessage() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile(
            registrationNumber = "REG/A",
            university = "Uni A",
            degreeProgram = "CS A",
            academicYear = 3,
            semester = 1,
            gpa = 3.9,
            bio = "Bio A"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addSkillProficiency("kotlin", ProficiencyLevel.ADVANCED)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addSkillProficiency("kotlin", ProficiencyLevel.EXPERT)

        val state = viewModel.uiState.value
        assertEquals("Skill is already added to your profile.", state.errorMessage)
        assertEquals(1, state.profile?.skillProficiencies?.size)
    }

    @Test
    fun updateSkillProficiency_existingSkill_updatesProficiencyLevel() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile(
            registrationNumber = "REG/A",
            university = "Uni A",
            degreeProgram = "CS A",
            academicYear = 3,
            semester = 1,
            gpa = 3.9,
            bio = "Bio A"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addSkillProficiency("kotlin", ProficiencyLevel.BEGINNER)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSkillProficiency("kotlin", ProficiencyLevel.EXPERT)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val skill = state.profile?.skillProficiencies?.find { it.skillId == "kotlin" }
        assertEquals(ProficiencyLevel.EXPERT, skill?.level)
    }

    @Test
    fun removeSkillProficiency_existingSkill_removesSkillFromProfile() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile(
            registrationNumber = "REG/A",
            university = "Uni A",
            degreeProgram = "CS A",
            academicYear = 3,
            semester = 1,
            gpa = 3.9,
            bio = "Bio A"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addSkillProficiency("kotlin", ProficiencyLevel.ADVANCED)
        viewModel.addSkillProficiency("android", ProficiencyLevel.INTERMEDIATE)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeSkillProficiency("kotlin")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val skills = state.profile?.skillProficiencies ?: emptyList()
        assertEquals(1, skills.size)
        assertEquals("android", skills.first().skillId)
    }

    @Test
    fun clearErrorAndSuccess_resetsMessages() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile(
            registrationNumber = "REG/A",
            university = "Uni A",
            degreeProgram = "CS A",
            academicYear = 3,
            semester = 1,
            gpa = 3.9,
            bio = "Bio A"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addSkillProficiency("kotlin", ProficiencyLevel.ADVANCED)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Skills updated successfully.", viewModel.uiState.value.successMessage)
        viewModel.clearSuccess()
        assertNull(viewModel.uiState.value.successMessage)

        viewModel.addSkillProficiency("kotlin", ProficiencyLevel.EXPERT) // error: duplicate
        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
