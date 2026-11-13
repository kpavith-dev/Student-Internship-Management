package com.internmatch.app.ui.search

import com.internmatch.app.data.repository.FakeAuthRepository
import com.internmatch.app.data.repository.FakeFirebaseBookmarkClient
import com.internmatch.app.data.repository.FakeFirebaseInternshipClient
import com.internmatch.app.data.repository.FirestoreBookmarkRepository
import com.internmatch.app.data.repository.FirestoreInternshipRepository
import com.internmatch.app.domain.model.ProficiencyLevel
import com.internmatch.app.domain.model.SkillProficiency
import com.internmatch.app.domain.model.StudentProfile
import com.internmatch.app.domain.model.UserRole
import com.internmatch.app.domain.service.DefaultMatchScoreProvider
import com.internmatch.app.ui.bookmark.BookmarkViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeInternshipClient: FakeFirebaseInternshipClient
    private lateinit var internshipRepository: FirestoreInternshipRepository

    private lateinit var fakeBookmarkClient: FakeFirebaseBookmarkClient
    private lateinit var bookmarkRepository: FirestoreBookmarkRepository

    private lateinit var matchScoreProvider: DefaultMatchScoreProvider
    private lateinit var searchViewModel: InternshipSearchViewModel
    private lateinit var bookmarkViewModel: BookmarkViewModel

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)

        fakeAuthRepository = FakeAuthRepository()
        fakeAuthRepository.signUp("student1@test.com", "pass1234", "Student One", UserRole.STUDENT)
        val currentUserId = fakeAuthRepository.getCurrentUser()?.id

        fakeInternshipClient = FakeFirebaseInternshipClient()
        fakeInternshipClient.internshipsData = mutableListOf(
            mapOf(
                "id" to "intern_colombo",
                "title" to "Senior Mobile Engineer",
                "companyName" to "Apex Tech",
                "description" to "Build Android and Kotlin applications",
                "domain" to "Software Engineering",
                "district" to "Colombo",
                "workMode" to "HYBRID",
                "status" to "APPROVED",
                "requiredSkills" to listOf(mapOf("skillId" to "kotlin", "weight" to 1.0))
            ),
            mapOf(
                "id" to "intern_kandy",
                "title" to "Data Scientist",
                "companyName" to "AI Core",
                "description" to "Machine Learning models",
                "domain" to "Data Science",
                "district" to "Kandy",
                "workMode" to "REMOTE",
                "status" to "APPROVED",
                "requiredSkills" to listOf(mapOf("skillId" to "python", "weight" to 1.0))
            )
        )
        internshipRepository = FirestoreInternshipRepository(fakeInternshipClient)

        fakeBookmarkClient = FakeFirebaseBookmarkClient()
        bookmarkRepository = FirestoreBookmarkRepository(
            bookmarkClient = fakeBookmarkClient,
            authenticatedUserIdProvider = { currentUserId }
        )

        matchScoreProvider = DefaultMatchScoreProvider()

        val mockProfileRepo = object : com.internmatch.app.domain.repository.StudentProfileRepository {
            override val currentStudentProfile = kotlinx.coroutines.flow.MutableStateFlow(null)
            override suspend fun getStudentProfile(studentId: String) = Result.success(
                StudentProfile(
                    studentId = studentId,
                    university = "University of Colombo",
                    degreeProgram = "BSc Computer Science",
                    academicYear = 4,
                    skillProficiencies = listOf(SkillProficiency("kotlin", ProficiencyLevel.ADVANCED))
                )
            )
            override suspend fun createStudentProfile(profile: StudentProfile) = Result.success(profile)
            override suspend fun updateStudentProfile(profile: StudentProfile) = Result.success(profile)
            override suspend fun updateSkillProficiencies(studentId: String, skillProficiencies: List<SkillProficiency>) = Result.success(skillProficiencies)
        }

        searchViewModel = InternshipSearchViewModel(
            internshipRepository = internshipRepository,
            bookmarkRepository = bookmarkRepository,
            studentProfileRepository = mockProfileRepo,
            authRepository = fakeAuthRepository,
            matchScoreProvider = matchScoreProvider
        )

        bookmarkViewModel = BookmarkViewModel(
            bookmarkRepository = bookmarkRepository,
            internshipRepository = internshipRepository,
            studentProfileRepository = mockProfileRepo,
            authRepository = fakeAuthRepository,
            matchScoreProvider = matchScoreProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `end to end flow search filter score calculation and bookmarking`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial loaded state with calculated match scores
        val initialSearchState = searchViewModel.uiState.value
        assertEquals(2, initialSearchState.internships.size)

        val colomboItem = initialSearchState.internships.find { it.internship.id == "intern_colombo" }!!
        assertEquals(100.0, colomboItem.matchScore!!, 0.001)

        val kandyItem = initialSearchState.internships.find { it.internship.id == "intern_kandy" }!!
        assertEquals(0.0, kandyItem.matchScore!!, 0.001)

        // Apply 50%+ min score filter
        searchViewModel.updateMinMatchScore(50.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val filteredSearchState = searchViewModel.uiState.value
        assertEquals(1, filteredSearchState.internships.size)
        assertEquals("intern_colombo", filteredSearchState.internships[0].internship.id)

        // Bookmark the matching internship
        searchViewModel.toggleBookmark("intern_colombo")
        testDispatcher.scheduler.advanceUntilIdle()

        // Load Bookmarks screen and verify persistence
        bookmarkViewModel.loadBookmarks()
        testDispatcher.scheduler.advanceUntilIdle()

        val bookmarkState = bookmarkViewModel.uiState.value
        assertEquals(1, bookmarkState.bookmarkedInternships.size)
        assertEquals("intern_colombo", bookmarkState.bookmarkedInternships[0].internship.id)
        assertTrue(bookmarkState.bookmarkedInternships[0].isBookmarked)
    }
}
