package com.internmatch.app.domain.repository

import com.internmatch.app.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing student bookmarks.
 * Path: users/{studentId}/bookmarks/{internshipId}
 * Access boundary: Strictly enforces authenticated UID ownership.
 */
interface BookmarkRepository {
    /**
     * Flow observing the active authenticated student's bookmarks.
     */
    val userBookmarks: Flow<List<Bookmark>>

    /**
     * Retrieves all saved bookmarks for the specified student.
     */
    suspend fun getBookmarks(studentId: String): Result<List<Bookmark>>

    /**
     * Adds an internship to the student's bookmarks.
     */
    suspend fun addBookmark(studentId: String, internshipId: String): Result<Bookmark>

    /**
     * Removes an internship from the student's bookmarks.
     */
    suspend fun removeBookmark(studentId: String, internshipId: String): Result<Unit>

    /**
     * Checks if an internship is currently bookmarked by the student.
     */
    suspend fun isBookmarked(studentId: String, internshipId: String): Result<Boolean>
}
