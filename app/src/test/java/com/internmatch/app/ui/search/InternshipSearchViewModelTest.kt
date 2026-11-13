package com.internmatch.app.ui.search

import com.internmatch.app.data.repository.FakeAuthRepository
import com.internmatch.app.data.repository.FakeFirebaseBookmarkClient
import com.internmatch.app.data.repository.FakeFirebaseInternshipClient
import com.internmatch.app.data.repository.FirestoreBookmarkRepository
import com.internmatch.app.data.repository.FirestoreInternshipRepository
import com.internmatch.app.domain.model.UserRole
import com.internmatch.app.domain.service.DefaultMatchScoreProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InternshipSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeInternshipClient: FakeFirebaseInternshipClient
    private lateinit var internshipRepository: FirestoreInternshipRepository

    private lateinit var fakeBookmarkClient: FakeFirebaseBookmarkClient
    private lateinit var bookmarkRepository: FirestoreBookmarkRepository

    private lateinit var matchScoreProvider: DefaultMatchScoreProvider
    private lateinit var viewModel: InternshipSearchViewModel

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)

        fakeAuthRepository = FakeAuthRepository()
        fakeAuthRepository.signUp("student@test.com", "password123", "Test Student", UserRole.STUDENT)

        fakeInternshipClient = FakeFirebaseInternshipClient()
        fakeInternshipClient.internshipsData = mutableListOf(
            mapOf(
                "id" to "intern_1",
                "title" to "Android Dev Intern",
                "companyName" to "Tech Corp",
                "description" to "Develop Kotlin mobile apps",
                "domain" to "Software Engineering",
                "district" to "Colombo",
                "workMode" to "REMOTE",
                "status" to "APPROVED",
                "requiredSkills" to listOf(mapOf("skillId" to "kotlin", "weight" to 1.0))
            ),
            mapOf(
                "id" to "intern_2",
                "title" to "Data Analyst Intern",
                "companyName" to "Data Inc",
                "description" to "Analyze data with Python",
                "domain" to "Data Science",
                "district" to "Kandy",
                "workMode" to "ON_SITE",
                "status" to "APPROVED",
                "requiredSkills" to listOf(mapOf("skillId" to "python", "weight" to 1.0))
            )
        )
        internshipRepository = FirestoreInternshipRepository(fakeInternshipClient)

        fakeBookmarkClient = FakeFirebaseBookmarkClient()
        val currentUserId = fakeAuthRepository.getCurrentUser()?.id
        bookmarkRepository = FirestoreBookmarkRepository(
            bookmarkClient = fakeBookmarkClient,
            authenticatedUserIdProvider = { currentUserId }
        )

        matchScoreProvider = DefaultMatchScoreProvider()

        // Create dummy profile repository fallback
        val dummyProfileRepo = object : com.internmatch.app.domain.repository.StudentProfileRepository {
            override val currentStudentProfile = kotlinx.coroutines.flow.MutableStateFlow(null)
            override suspend fun getStudentProfile(studentId: String) = Result.success(null)
            override suspend fun createStudentProfile(profile: com.internmatch.app.domain.model.StudentProfile) = Result.success(profile)
            override suspend fun updateStudentProfile(profile: com.internmatch.app.domain.model.StudentProfile) = Result.success(profile)
            override suspend fun updateSkillProficiencies(studentId: String, skillProficiencies: List<com.internmatch.app.domain.model.SkillProficiency>) = Result.success(skillProficiencies)
        }

        viewModel = InternshipSearchViewModel(
            internshipRepository = internshipRepository,
            bookmarkRepository = bookmarkRepository,
            studentProfileRepository = dummyProfileRepo,
            authRepository = fakeAuthRepository,
            matchScoreProvider = matchScoreProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadData populates internships list successfully`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.internships.size)
        assertEquals(listOf("Data Science", "Software Engineering"), state.availableDomains)
    }

    @Test
    fun `updateQuery filters internships list by title keyword`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateQuery("Android")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.internships.size)
        assertEquals("Android Dev Intern", state.internships[0].internship.title)
    }

    @Test
    fun `updateDomain filters internships list by domain`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateDomain("Data Science")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.internships.size)
        assertEquals("intern_2", state.internships[0].internship.id)
    }

    @Test
    fun `clearFilters resets all filter state`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateQuery("Android")
        viewModel.updateDomain("Software Engineering")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.internships.size)
        assertFalse(state.filterState.hasActiveFilters)
    }

    @Test
    fun `toggleBookmark toggles bookmark status for an internship`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleBookmark("intern_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val item = state.internships.find { it.internship.id == "intern_1" }
        assertTrue(item!!.isBookmarked)
    }
}
