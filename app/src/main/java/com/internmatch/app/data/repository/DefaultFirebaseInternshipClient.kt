package com.internmatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Production implementation of [FirebaseInternshipClient] using Firebase Cloud Firestore SDK.
 * Queries collection /internships filtering by status = "APPROVED".
 */
class DefaultFirebaseInternshipClient(
    private val firestore: FirebaseFirestore
) : FirebaseInternshipClient {

    override val isConfigured: Boolean
        get() = true

    override suspend fun getApprovedInternships(limit: Long): List<Map<String, Any?>> {
        val querySnapshot = firestore.collection("internships")
            .whereEqualTo("status", "APPROVED")
            .limit(limit)
            .get()
            .await()

        return querySnapshot.documents.mapNotNull { doc ->
            val data = doc.data?.toMutableMap() ?: return@mapNotNull null
            data["id"] = doc.id
            data
        }
    }
}
