package com.internmatch.app.core.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.internmatch.app.data.repository.DefaultFirebaseAuthClient
import com.internmatch.app.data.repository.DefaultFirebaseBookmarkClient
import com.internmatch.app.data.repository.DefaultFirebaseApplicationClient
import com.internmatch.app.data.repository.DefaultFirebaseInternshipClient
import com.internmatch.app.data.repository.DefaultFirebaseStudentProfileClient
import com.internmatch.app.data.repository.DefaultSkillRepository
import com.internmatch.app.data.repository.FirebaseAuthRepository
import com.internmatch.app.data.repository.FirebaseBookmarkClient
import com.internmatch.app.data.repository.FirebaseApplicationClient
import com.internmatch.app.data.repository.FirebaseInternshipClient
import com.internmatch.app.data.repository.FirebaseStudentProfileClient
import com.internmatch.app.data.repository.FirestoreBookmarkRepository
import com.internmatch.app.data.repository.FirestoreApplicationRepository
import com.internmatch.app.data.repository.FirestoreInternshipRepository
import com.internmatch.app.data.repository.FirestoreStudentProfileRepository
import com.internmatch.app.domain.repository.ApplicationRepository
import com.internmatch.app.domain.repository.AuthRepository
import com.internmatch.app.domain.repository.BookmarkRepository
import com.internmatch.app.domain.repository.InternshipRepository
import com.internmatch.app.domain.repository.SkillRepository
import com.internmatch.app.domain.repository.StudentProfileRepository
import com.internmatch.app.domain.service.DefaultMatchScoreProvider
import com.internmatch.app.domain.service.MatchScoreProvider

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
    val internshipRepository: InternshipRepository
    val bookmarkRepository: BookmarkRepository
    val applicationRepository: ApplicationRepository
    val matchScoreProvider: MatchScoreProvider
}

/**
 * Fallback clients used when Firebase SDK initialization is unconfigured or unavailable.
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

private class UnconfiguredFirebaseInternshipClient : FirebaseInternshipClient {
    override val isConfigured: Boolean get() = false
    override suspend fun getApprovedInternships(limit: Long): List<Map<String, Any?>> = emptyList()
}

private class UnconfiguredFirebaseBookmarkClient : FirebaseBookmarkClient {
    override val isConfigured: Boolean get() = false
    override suspend fun getBookmarks(studentId: String): List<Map<String, Any?>> = emptyList()
    override suspend fun addBookmark(studentId: String, internshipId: String, data: Map<String, Any?>) {
        throw IllegalStateException("Firebase Firestore is unconfigured.")
    }
    override suspend fun removeBookmark(studentId: String, internshipId: String) {
        throw IllegalStateException("Firebase Firestore is unconfigured.")
    }
}

private class UnconfiguredFirebaseApplicationClient : FirebaseApplicationClient {
    override val isConfigured: Boolean get() = false
    override suspend fun submitApplicationTransaction(
        applicationId: String,
        applicationData: Map<String, Any?>,
        historyData: Map<String, Any?>
    ) {
        throw IllegalStateException("Firebase Firestore is unconfigured.")
    }
    override suspend fun hasStudentApplied(studentId: String, internshipId: String): Boolean = false
    override suspend fun getStudentApplications(studentId: String): List<Map<String, Any?>> = emptyList()
    override suspend fun getApplicationById(applicationId: String): Map<String, Any?>? = null
    override suspend fun getApplicationHistory(applicationId: String): List<Map<String, Any?>> = emptyList()
    override suspend fun withdrawApplicationTransaction(
        applicationId: String,
        studentId: String,
        historyData: Map<String, Any?>
    ) {
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

    override val internshipRepository: InternshipRepository by lazy {
        val appContext = context.applicationContext
        val isFirebaseInitialized = try {
            FirebaseApp.getApps(appContext).isNotEmpty()
        } catch (_: Throwable) {
            false
        }

        val client: FirebaseInternshipClient = if (isFirebaseInitialized) {
            try {
                DefaultFirebaseInternshipClient(FirebaseFirestore.getInstance())
            } catch (_: Throwable) {
                UnconfiguredFirebaseInternshipClient()
            }
        } else {
            UnconfiguredFirebaseInternshipClient()
        }

        FirestoreInternshipRepository(internshipClient = client)
    }

    override val bookmarkRepository: BookmarkRepository by lazy {
        val appContext = context.applicationContext
        val isFirebaseInitialized = try {
            FirebaseApp.getApps(appContext).isNotEmpty()
        } catch (_: Throwable) {
            false
        }

        val client: FirebaseBookmarkClient = if (isFirebaseInitialized) {
            try {
                DefaultFirebaseBookmarkClient(FirebaseFirestore.getInstance())
            } catch (_: Throwable) {
                UnconfiguredFirebaseBookmarkClient()
            }
        } else {
            UnconfiguredFirebaseBookmarkClient()
        }

        FirestoreBookmarkRepository(
            bookmarkClient = client,
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

    override val applicationRepository: ApplicationRepository by lazy {
        val appContext = context.applicationContext
        val isFirebaseInitialized = try {
            FirebaseApp.getApps(appContext).isNotEmpty()
        } catch (_: Throwable) {
            false
        }

        val client: FirebaseApplicationClient = if (isFirebaseInitialized) {
            try {
                DefaultFirebaseApplicationClient(FirebaseFirestore.getInstance())
            } catch (_: Throwable) {
                UnconfiguredFirebaseApplicationClient()
            }
        } else {
            UnconfiguredFirebaseApplicationClient()
        }

        FirestoreApplicationRepository(
            applicationClient = client,
            internshipRepository = internshipRepository,
            studentProfileRepository = studentProfileRepository,
            matchScoreProvider = matchScoreProvider,
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

    override val matchScoreProvider: MatchScoreProvider by lazy {
        DefaultMatchScoreProvider()
    }
}
