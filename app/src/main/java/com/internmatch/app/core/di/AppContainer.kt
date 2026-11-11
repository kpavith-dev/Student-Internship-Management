package com.internmatch.app.core.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.internmatch.app.data.repository.DefaultFirebaseAuthClient
import com.internmatch.app.data.repository.FirebaseAuthRepository
import com.internmatch.app.domain.repository.AuthRepository

/**
 * Dependency injection container for the application.
 * Follows the approved Manual Dependency Injection pattern (no Hilt/Koin).
 *
 * Strictly reflection-free. Uses typed Firebase SDK APIs for environment checks.
 */
interface AppContainer {
    val authRepository: AuthRepository
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
}
