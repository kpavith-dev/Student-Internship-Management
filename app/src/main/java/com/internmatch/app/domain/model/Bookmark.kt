package com.internmatch.app.domain.model

/**
 * Bookmark domain entity representing a saved internship by a student.
 * Firestore Path: users/{studentId}/bookmarks/{internshipId}
 * Uses [internshipId] as the document ID for atomic uniqueness and idempotency.
 */
data class Bookmark(
    val internshipId: String,
    val savedAt: Long = System.currentTimeMillis()
)
