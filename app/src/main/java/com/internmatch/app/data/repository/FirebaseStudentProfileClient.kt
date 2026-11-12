package com.internmatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Strongly-typed abstraction over Cloud Firestore SDK APIs for Student Profile operations.
 * Allows reflection-free, testable Firestore data access with atomic transactions and owner verification.
 */
interface FirebaseStudentProfileClient {
    val isConfigured: Boolean
    suspend fun getProfile(studentId: String): Map<String, Any?>?
    suspend fun createProfile(studentId: String, data: Map<String, Any?>)
    suspend fun updateProfile(studentId: String, data: Map<String, Any?>)
    suspend fun updateSkillProficiencies(studentId: String, skillProficienciesData: List<Map<String, Any?>>)
}

/**
 * Production implementation of [FirebaseStudentProfileClient] using official Firebase Firestore SDK transactions.
 *
 * Strictly verifies document path UID against stored owner UID, rejects legacy/unowned documents,
 * enforces owner UID immutability, and uses [SetOptions.merge] to preserve unrelated fields.
 */
class DefaultFirebaseStudentProfileClient(
    private val firestore: FirebaseFirestore
) : FirebaseStudentProfileClient {

    override val isConfigured: Boolean
        get() = true

    override suspend fun getProfile(studentId: String): Map<String, Any?>? {
        val snapshot = firestore.collection(COLLECTION_STUDENT_PROFILES)
            .document(studentId)
            .get()
            .await()

        if (!snapshot.exists()) {
            return null
        }

        val storedOwnerId = snapshot.get(FIELD_STUDENT_ID) as? String
        if (storedOwnerId.isNullOrBlank() || storedOwnerId != studentId) {
            throw SecurityException("Stored document owner UID '$storedOwnerId' is missing or does not match requested document path UID '$studentId'. Legacy or unowned documents are rejected.")
        }

        return snapshot.data
    }

    override suspend fun createProfile(studentId: String, data: Map<String, Any?>) {
        val dataOwnerId = data[FIELD_STUDENT_ID] as? String
        if (dataOwnerId.isNullOrBlank() || dataOwnerId != studentId) {
            throw IllegalArgumentException("Owner UID mismatch or missing in create payload: data studentId '$dataOwnerId' != path studentId '$studentId'.")
        }

        firestore.runTransaction { transaction ->
            val ref = firestore.collection(COLLECTION_STUDENT_PROFILES).document(studentId)
            val snapshot = transaction.get(ref)
            if (snapshot.exists()) {
                throw IllegalStateException("Profile already exists for student UID '$studentId'. Use updateProfile instead.")
            }
            transaction.set(ref, data)
        }.await()
    }

    override suspend fun updateProfile(studentId: String, data: Map<String, Any?>) {
        if (data.containsKey(FIELD_STUDENT_ID)) {
            val dataOwnerId = data[FIELD_STUDENT_ID] as? String
            if (dataOwnerId.isNullOrBlank() || dataOwnerId != studentId) {
                throw IllegalArgumentException("Invalid or mismatched studentId in update payload: key is present but value '${data[FIELD_STUDENT_ID]}' is not a non-blank String matching '$studentId'.")
            }
        }

        firestore.runTransaction { transaction ->
            val ref = firestore.collection(COLLECTION_STUDENT_PROFILES).document(studentId)
            val snapshot = transaction.get(ref)
            if (!snapshot.exists()) {
                throw NoSuchElementException("Student profile does not exist for UID '$studentId'. Use createProfile instead.")
            }

            val storedOwnerId = snapshot.get(FIELD_STUDENT_ID) as? String
            if (storedOwnerId.isNullOrBlank() || storedOwnerId != studentId) {
                throw SecurityException("Stored document owner UID '$storedOwnerId' is missing or does not match document path UID '$studentId'. Legacy or corrupted documents without owner UID cannot be modified.")
            }

            transaction.set(ref, data, SetOptions.merge())
        }.await()
    }

    override suspend fun updateSkillProficiencies(
        studentId: String,
        skillProficienciesData: List<Map<String, Any?>>
    ) {
        firestore.runTransaction { transaction ->
            val ref = firestore.collection(COLLECTION_STUDENT_PROFILES).document(studentId)
            val snapshot = transaction.get(ref)
            if (!snapshot.exists()) {
                throw NoSuchElementException("Student profile does not exist for UID '$studentId'. Create a profile before updating skills.")
            }

            val storedOwnerId = snapshot.get(FIELD_STUDENT_ID) as? String
            if (storedOwnerId.isNullOrBlank() || storedOwnerId != studentId) {
                throw SecurityException("Stored document owner UID '$storedOwnerId' is missing or does not match document path UID '$studentId'.")
            }

            val data = mapOf(FIELD_SKILL_PROFICIENCIES to skillProficienciesData)
            transaction.set(ref, data, SetOptions.merge())
        }.await()
    }

    companion object {
        const val COLLECTION_STUDENT_PROFILES = "studentProfiles"
        const val FIELD_STUDENT_ID = "studentId"
        const val FIELD_SKILL_PROFICIENCIES = "skillProficiencies"
    }
}
