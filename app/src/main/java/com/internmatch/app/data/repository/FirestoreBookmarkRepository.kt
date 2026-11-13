package com.internmatch.app.data.repository

import com.internmatch.app.domain.model.Bookmark
import com.internmatch.app.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository implementation for student bookmarks.
 * Strictly enforces authenticated UID ownership at the data boundary.
 * Interacts with path: users/{studentId}/bookmarks/{internshipId}
 */
class FirestoreBookmarkRepository(
    private val bookmarkClient: FirebaseBookmarkClient,
    private val authenticatedUserIdProvider: () -> String?
) : BookmarkRepository {

    private val _userBookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    override val userBookmarks: Flow<List<Bookmark>> = _userBookmarks.asStateFlow()

    private fun validateOwnership(studentId: String): String? {
        val authUid = authenticatedUserIdProvider()
        if (authUid.isNull_or_blank()) return null
        if (authUid != studentId) return null
        return authUid
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.isBlank()
    }

    override suspend fun getBookmarks(studentId: String): Result<List<Bookmark>> {
        val authUid = validateOwnership(studentId)
            ?: return Result.failure(SecurityException("Access denied: Unauthenticated or non-matching UID for studentId=$studentId"))

        return try {
            if (!bookmarkClient.isConfigured) {
                return Result.success(_userBookmarks.value)
            }
            val rawList = bookmarkClient.getBookmarks(authUid)
            val bookmarks = rawList.mapNotNull { mapToBookmark(it) }
            _userBookmarks.value = bookmarks
            Result.success(bookmarks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addBookmark(studentId: String, internshipId: String): Result<Bookmark> {
        val authUid = validateOwnership(studentId)
            ?: return Result.failure(SecurityException("Access denied: Unauthenticated or non-matching UID for studentId=$studentId"))

        return try {
            val now = System.currentTimeMillis()
            val bookmark = Bookmark(internshipId = internshipId, savedAt = now)
            val data = mapOf(
                "internshipId" to internshipId,
                "savedAt" to now
            )

            if (bookmarkClient.isConfigured) {
                bookmarkClient.addBookmark(authUid, internshipId, data)
            }

            val current = _userBookmarks.value.toMutableList()
            current.removeAll { it.internshipId == internshipId }
            current.add(bookmark)
            _userBookmarks.value = current

            Result.success(bookmark)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeBookmark(studentId: String, internshipId: String): Result<Unit> {
        val authUid = validateOwnership(studentId)
            ?: return Result.failure(SecurityException("Access denied: Unauthenticated or non-matching UID for studentId=$studentId"))

        return try {
            if (bookmarkClient.isConfigured) {
                bookmarkClient.removeBookmark(authUid, internshipId)
            }

            val current = _userBookmarks.value.toMutableList()
            current.removeAll { it.internshipId == internshipId }
            _userBookmarks.value = current

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isBookmarked(studentId: String, internshipId: String): Result<Boolean> {
        val authUid = validateOwnership(studentId)
            ?: return Result.failure(SecurityException("Access denied: Unauthenticated or non-matching UID for studentId=$studentId"))

        return try {
            val exists = _userBookmarks.value.any { it.internshipId == internshipId }
            if (exists) return Result.success(true)

            if (_userBookmarks.value.isEmpty() && bookmarkClient.isConfigured) {
                val fetchResult = getBookmarks(authUid)
                if (fetchResult.isSuccess) {
                    val isFound = fetchResult.getOrDefault(emptyList()).any { it.internshipId == internshipId }
                    return Result.success(isFound)
                }
            }

            Result.success(exists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToBookmark(data: Map<String, Any?>): Bookmark? {
        val internshipId = data["internshipId"] as? String ?: return null
        val savedAt = (data["savedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        return Bookmark(internshipId = internshipId, savedAt = savedAt)
    }
}
