package com.internmatch.app.ui.bookmark

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
class BookmarkViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeInternshipClient: FakeFirebaseInternshipClient
    private lateinit var internshipRepository: FirestoreInternshipRepository

    private lateinit var fakeBookmarkClient: FakeFirebaseBookmarkClient
    private lateinit var bookmarkRepository: FirestoreBookmarkRepository

    private lateinit var matchScoreProvider: DefaultMatchScoreProvider
    private lateinit var viewModel: BookmarkViewModel

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)

        fakeAuthRepository = FakeAuthRepository()
        fakeAuthRepository.signUp("student@test.com", "password123", "Test Student", UserRole.STUDENT)
        val currentUserId = fakeAuthRepository.getCurrentUser()?.id

        fakeInternshipClient = FakeFirebaseInternshipClient()
        fakeInternshipClient.internshipsData = mutableListOf(
            mapOf(
                "id" to "intern_1",
                "title" to "Android Dev Intern",
                "companyName" to "Tech Corp",
                "status" to "APPROVED"
            )
        )
        internshipRepository = FirestoreInternshipRepository(fakeInternshipClient)

        fakeBookmarkClient = FakeFirebaseBookmarkClient()
        fakeBookmarkClient.storage[currentUserId!!] = mutableMapOf(
            "intern_1" to mapOf("internshipId" to "intern_1", "savedAt" to 1000L)
        )
        bookmarkRepository = FirestoreBookmarkRepository(
            bookmarkClient = fakeBookmarkClient,
            authenticatedUserIdProvider = { currentUserId }
        )

        matchScoreProvider = DefaultMatchScoreProvider()

        val dummyProfileRepo = object : com.internmatch.app.domain.repository.StudentProfileRepository {
            override val currentStudentProfile = kotlinx.coroutines.flow.MutableStateFlow(null)
            override suspend fun getStudentProfile(studentId: String) = Result.success(null)
            override suspend fun createStudentProfile(profile: com.internmatch.app.domain.model.StudentProfile) = Result.success(profile)
            override suspend fun updateStudentProfile(profile: com.internmatch.app.domain.model.StudentProfile) = Result.success(profile)
            override suspend fun updateSkillProficiencies(studentId: String, skillProficiencies: List<com.internmatch.app.domain.model.SkillProficiency>) = Result.success(skillProficiencies)
        }

        viewModel = BookmarkViewModel(
            bookmarkRepository = bookmarkRepository,
            internshipRepository = internshipRepository,
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
    fun `loadBookmarks populates saved bookmarked internships`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.bookmarkedInternships.size)
        assertEquals("intern_1", state.bookmarkedInternships[0].internship.id)
    }

    @Test
    fun `removeBookmark removes internship from bookmarked list`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.removeBookmark("intern_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.bookmarkedInternships.isEmpty())
        assertTrue(state.isEmpty)
    }
}
