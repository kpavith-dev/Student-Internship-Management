package com.internmatch.app.data.repository

/**
 * Abstraction over Firebase Firestore SDK operations for Internship listings.
 * Enables decoupling domain repositories from direct Firebase SDK calls and allows testing with fakes.
 */
interface FirebaseInternshipClient {
    /**
     * Checks if Firebase Firestore SDK is available and configured.
     */
    val isConfigured: Boolean

    /**
     * Retrieves raw document maps for internships with status = APPROVED from Cloud Firestore.
     */
    suspend fun getApprovedInternships(limit: Long = 200): List<Map<String, Any?>>
}
