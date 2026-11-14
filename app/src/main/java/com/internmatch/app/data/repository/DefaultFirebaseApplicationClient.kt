package com.internmatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.internmatch.app.domain.model.ApplicationException
import kotlinx.coroutines.tasks.await

/**
 * Production implementation of [FirebaseApplicationClient] using Firebase Cloud Firestore SDK.
 * Handles atomic transactions for submission and withdrawal, and chronological history streaming.
 */
class DefaultFirebaseApplicationClient(
    private val firestore: FirebaseFirestore
) : FirebaseApplicationClient {

    override val isConfigured: Boolean
        get() = true

    override suspend fun submitApplicationTransaction(
        applicationId: String,
        applicationData: Map<String, Any?>,
        historyData: Map<String, Any?>
    ) {
        val appRef = firestore.collection("applications").document(applicationId)
        val historyId = historyData["id"] as? String ?: "hist_${System.currentTimeMillis()}"
        val historyRef = appRef.collection("history").document(historyId)

        try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(appRef)
                if (snapshot.exists()) {
                    throw ApplicationException.AlreadyApplied
                }
                transaction.set(appRef, applicationData)
                transaction.set(historyRef, historyData)
            }.await()
        } catch (e: Exception) {
            if (e is ApplicationException) throw e
            throw ApplicationException.StorageError("Failed to submit application: ${e.message}", e)
        }
    }

    override suspend fun hasStudentApplied(studentId: String, internshipId: String): Boolean {
        val appId = "${internshipId}_${studentId}"
        val snapshot = firestore.collection("applications").document(appId).get().await()
        return snapshot.exists()
    }

    override suspend fun getStudentApplications(studentId: String): List<Map<String, Any?>> {
        val querySnapshot = firestore.collection("applications")
            .whereEqualTo("studentId", studentId)
            .get()
            .await()

        return querySnapshot.documents.mapNotNull { doc ->
            val data = doc.data?.toMutableMap() ?: return@mapNotNull null
            data["id"] = doc.id
            data
        }
    }

    override suspend fun getApplicationById(applicationId: String): Map<String, Any?>? {
        val doc = firestore.collection("applications").document(applicationId).get().await()
        if (!doc.exists()) return null
        val data = doc.data?.toMutableMap() ?: return null
        data["id"] = doc.id
        return data
    }

    override suspend fun getApplicationHistory(applicationId: String): List<Map<String, Any?>> {
        val querySnapshot = firestore.collection("applications")
            .document(applicationId)
            .collection("history")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .await()

        return querySnapshot.documents.mapNotNull { doc ->
            val data = doc.data?.toMutableMap() ?: return@mapNotNull null
            data["id"] = doc.id
            data
        }
    }

    override suspend fun withdrawApplicationTransaction(
        applicationId: String,
        studentId: String,
        historyData: Map<String, Any?>
    ) {
        val appRef = firestore.collection("applications").document(applicationId)
        val historyId = historyData["id"] as? String ?: "hist_${System.currentTimeMillis()}"
        val historyRef = appRef.collection("history").document(historyId)

        try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(appRef)
                if (!snapshot.exists()) {
                    throw ApplicationException.ApplicationNotFound
                }
                val ownerId = snapshot.getString("studentId") ?: ""
                if (ownerId != studentId) {
                    throw ApplicationException.UnauthorizedAccess
                }
                val currentStatus = snapshot.getString("status") ?: ""
                if (currentStatus == "WITHDRAWN") {
                    return@runTransaction
                }
                if (currentStatus !in listOf("SUBMITTED", "UNDER_REVIEW", "SHORTLISTED")) {
                    throw ApplicationException.NotEligibleForWithdrawal
                }

                val now = System.currentTimeMillis()
                transaction.update(appRef, mapOf("status" to "WITHDRAWN", "updatedAt" to now))
                transaction.set(historyRef, historyData)
            }.await()
        } catch (e: Exception) {
            if (e is ApplicationException) throw e
            throw ApplicationException.StorageError("Failed to withdraw application: ${e.message}", e)
        }
    }
}
