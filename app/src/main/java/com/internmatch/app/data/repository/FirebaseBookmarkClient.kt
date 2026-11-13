package com.internmatch.app.data.repository

/**
 * Abstraction over Firebase Firestore SDK operations for Student Bookmarks.
 * Target Firestore path: users/{studentId}/bookmarks/{internshipId}
 */
interface FirebaseBookmarkClient {
    /**
     * Checks if Firebase Firestore SDK is available and configured.
     */
    val isConfigured: Boolean

    /**
     * Retrieves all raw bookmark document maps under users/{studentId}/bookmarks.
     */
    suspend fun getBookmarks(studentId: String): List<Map<String, Any?>>

    /**
     * Saves or updates a bookmark document under users/{studentId}/bookmarks/{internshipId}.
     */
    suspend fun addBookmark(studentId: String, internshipId: String, data: Map<String, Any?>)

    /**
     * Deletes a bookmark document under users/{studentId}/bookmarks/{internshipId}.
     */
    suspend fun removeBookmark(studentId: String, internshipId: String)
}
