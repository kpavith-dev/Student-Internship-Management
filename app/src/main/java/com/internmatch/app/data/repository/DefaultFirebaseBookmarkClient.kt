package com.internmatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Production implementation of [FirebaseBookmarkClient] using Firebase Cloud Firestore SDK.
 * Interacts with path: users/{studentId}/bookmarks/{internshipId}
 */
class DefaultFirebaseBookmarkClient(
    private val firestore: FirebaseFirestore
) : FirebaseBookmarkClient {

    override val isConfigured: Boolean
        get() = true

    override suspend fun getBookmarks(studentId: String): List<Map<String, Any?>> {
        val querySnapshot = firestore.collection("users")
            .document(studentId)
            .collection("bookmarks")
            .get()
            .await()

        return querySnapshot.documents.mapNotNull { doc ->
            val data = doc.data?.toMutableMap() ?: return@mapNotNull null
            data["internshipId"] = doc.id
            data
        }
    }

    override suspend fun addBookmark(studentId: String, internshipId: String, data: Map<String, Any?>) {
        firestore.collection("users")
            .document(studentId)
            .collection("bookmarks")
            .document(internshipId)
            .set(data, SetOptions.merge())
            .await()
    }

    override suspend fun removeBookmark(studentId: String, internshipId: String) {
        firestore.collection("users")
            .document(studentId)
            .collection("bookmarks")
            .document(internshipId)
            .delete()
            .await()
    }
}
