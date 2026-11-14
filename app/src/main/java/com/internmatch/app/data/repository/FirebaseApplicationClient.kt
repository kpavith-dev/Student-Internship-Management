package com.internmatch.app.data.repository

/**
 * Abstraction over Firebase Firestore SDK operations for Student Applications (FR18, FR19, FR21).
 * Interacts with path: applications/{applicationId} and history subcollection applications/{applicationId}/history/{historyId}
 */
interface FirebaseApplicationClient {
    /**
     * Checks if Firebase Firestore SDK is available and configured.
     */
    val isConfigured: Boolean

    /**
     * Atomically submits an application and its initial SUBMITTED history event inside a Firestore transaction.
     */
    suspend fun submitApplicationTransaction(
        applicationId: String,
        applicationData: Map<String, Any?>,
        historyData: Map<String, Any?>
    )

    /**
     * Checks if an application document exists for studentId and internshipId.
     */
    suspend fun hasStudentApplied(studentId: String, internshipId: String): Boolean

    /**
     * Retrieves raw application documents for a given student UID.
     */
    suspend fun getStudentApplications(studentId: String): List<Map<String, Any?>>

    /**
     * Retrieves a single raw application document by ID.
     */
    suspend fun getApplicationById(applicationId: String): Map<String, Any?>?

    /**
     * Retrieves status history documents for an application ordered chronologically.
     */
    suspend fun getApplicationHistory(applicationId: String): List<Map<String, Any?>>

    /**
     * Atomically withdraws an application and appends a WITHDRAWN status history event inside a transaction.
     */
    suspend fun withdrawApplicationTransaction(
        applicationId: String,
        studentId: String,
        historyData: Map<String, Any?>
    )
}
