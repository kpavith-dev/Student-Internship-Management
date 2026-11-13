package com.internmatch.app.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeFirebaseBookmarkClient : FirebaseBookmarkClient {
    override var isConfigured: Boolean = true
    val storage = mutableMapOf<String, MutableMap<String, Map<String, Any?>>>()
    var shouldFail: Boolean = false

    override suspend fun getBookmarks(studentId: String): List<Map<String, Any?>> {
        if (shouldFail) throw RuntimeException("Firestore bookmark error")
        return storage[studentId]?.values?.toList() ?: emptyList()
    }

    override suspend fun addBookmark(studentId: String, internshipId: String, data: Map<String, Any?>) {
        if (shouldFail) throw RuntimeException("Firestore bookmark error")
        val userMap = storage.getOrPut(studentId) { mutableMapOf() }
        userMap[internshipId] = data
    }

    override suspend fun removeBookmark(studentId: String, internshipId: String) {
        if (shouldFail) throw RuntimeException("Firestore bookmark error")
        storage[studentId]?.remove(internshipId)
    }
}

class FirestoreBookmarkRepositoryTest {

    private lateinit var fakeClient: FakeFirebaseBookmarkClient
    private var currentAuthUid: String? = "student_123"

    private lateinit var repository: FirestoreBookmarkRepository

    @Before
    fun setUp() {
        fakeClient = FakeFirebaseBookmarkClient()
        currentAuthUid = "student_123"
        repository = FirestoreBookmarkRepository(
            bookmarkClient = fakeClient,
            authenticatedUserIdProvider = { currentAuthUid }
        )
    }

    @Test
    fun `addBookmark saves bookmark with deterministic internshipId document ID`() = runTest {
        val result = repository.addBookmark("student_123", "intern_abc")
        assertTrue(result.isSuccess)
        val bookmark = result.getOrNull()!!
        assertEquals("intern_abc", bookmark.internshipId)

        val storedData = fakeClient.storage["student_123"]?.get("intern_abc")
        assertEquals("intern_abc", storedData?.get("internshipId"))
    }

    @Test
    fun `removeBookmark removes bookmark cleanly from storage`() = runTest {
        repository.addBookmark("student_123", "intern_abc")
        val removeResult = repository.removeBookmark("student_123", "intern_abc")
        assertTrue(removeResult.isSuccess)

        val isBookmarked = repository.isBookmarked("student_123", "intern_abc").getOrDefault(false)
        assertFalse(isBookmarked)
    }

    @Test
    fun `repeated addBookmark operation is idempotent`() = runTest {
        repository.addBookmark("student_123", "intern_abc")
        repository.addBookmark("student_123", "intern_abc")

        val bookmarksResult = repository.getBookmarks("student_123")
        assertTrue(bookmarksResult.isSuccess)
        assertEquals(1, bookmarksResult.getOrNull()!!.size)
    }

    @Test
    fun `addBookmark fails with SecurityException when user is unauthenticated`() = runTest {
        currentAuthUid = null
        val result = repository.addBookmark("student_123", "intern_abc")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `addBookmark fails with SecurityException when studentId does not match auth UID`() = runTest {
        currentAuthUid = "student_123"
        val result = repository.addBookmark("student_other", "intern_abc")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `getBookmarks returns only authenticated user bookmarks`() = runTest {
        fakeClient.storage["student_123"] = mutableMapOf(
            "intern_1" to mapOf("internshipId" to "intern_1", "savedAt" to 1000L)
        )
        fakeClient.storage["student_other"] = mutableMapOf(
            "intern_99" to mapOf("internshipId" to "intern_99", "savedAt" to 2000L)
        )

        val result = repository.getBookmarks("student_123")
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        assertEquals("intern_1", list[0].internshipId)
    }

    @Test
    fun `isBookmarked evaluates true for saved bookmark and false otherwise`() = runTest {
        repository.addBookmark("student_123", "intern_xyz")
        assertTrue(repository.isBookmarked("student_123", "intern_xyz").getOrDefault(false))
        assertFalse(repository.isBookmarked("student_123", "intern_missing").getOrDefault(true))
    }
}
