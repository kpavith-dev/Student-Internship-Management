package com.internmatch.app.core.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.internmatch.app.data.repository.DefaultFirebaseAuthClient
import com.internmatch.app.data.repository.DefaultFirebaseStudentProfileClient
import com.internmatch.app.data.repository.DefaultSkillRepository
import com.internmatch.app.data.repository.FirebaseStudentProfileClient
import com.internmatch.app.data.repository.FirebaseAuthRepository
import com.internmatch.app.data.repository.FirestoreStudentProfileRepository
import com.internmatch.app.domain.repository.AuthRepository
import com.internmatch.app.domain.repository.SkillRepository
import com.internmatch.app.domain.repository.StudentProfileRepository

/**
 * Dependency injection container for the application.
 * Follows the approved Manual Dependency Injection pattern (no Hilt/Koin).
 *
 * Strictly reflection-free. Uses typed Firebase SDK APIs for environment checks.
 */
interface AppContainer {
    val authRepository: AuthRepository
    val studentProfileRepository: StudentProfileRepository
    val skillRepository: SkillRepository
}

/**
 * Fallback client used when Firebase SDK initialization is unconfigured or unavailable.
 */
private class UnconfiguredFirebaseStudentProfileClient : FirebaseStudentProfileClient {
    override val isConfigured: Boolean get() = false
    override suspend fun getProfile(studentId: String): Map<String, Any?>? = null
    override suspend fun createProfile(studentId: String, data: Map<String, Any?>) {
        throw IllegalStateException("Firebase Firestore is unconfigured.")
    }
    override suspend fun updateProfile(studentId: String, data: Map<String, Any?>) {
        throw IllegalStateException("Firebase Firestore is unconfigured.")
    }
    override suspend fun updateSkillProficiencies(studentId: String, skillProficienciesData: List<Map<String, Any?>>) {
        throw IllegalStateException("Firebase Firestore is unconfigured.")
    }
}

/**
 * Default production implementation of [AppContainer].
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    override val authRepository: AuthRepository by lazy {
        val appContext = context.applicationContext
        val isFirebaseInitialized = try {
            FirebaseApp.getApps(appContext).isNotEmpty()
        } catch (_: Throwable) {
            false
        }

        val firebaseAuthClient = if (isFirebaseInitialized) {
            try {
                DefaultFirebaseAuthClient(FirebaseAuth.getInstance())
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }

        FirebaseAuthRepository(
            firebaseAuthClient = firebaseAuthClient
        )
    }

    override val studentProfileRepository: StudentProfileRepository by lazy {
        val appContext = context.applicationContext
        val isFirebaseInitialized = try {
            FirebaseApp.getApps(appContext).isNotEmpty()
        } catch (_: Throwable) {
            false
        }

        val profileClient: FirebaseStudentProfileClient = if (isFirebaseInitialized) {
            try {
                DefaultFirebaseStudentProfileClient(FirebaseFirestore.getInstance())
            } catch (_: Throwable) {
                UnconfiguredFirebaseStudentProfileClient()
            }
        } else {
            UnconfiguredFirebaseStudentProfileClient()
        }

        FirestoreStudentProfileRepository(
            profileClient = profileClient,
            authenticatedUserIdProvider = {
                if (isFirebaseInitialized) {
                    try {
                        FirebaseAuth.getInstance().currentUser?.uid
                    } catch (_: Throwable) {
                        null
                    }
                } else {
                    null
                }
            }
        )
    }

    override val skillRepository: SkillRepository by lazy {
        DefaultSkillRepository()
    }
}
