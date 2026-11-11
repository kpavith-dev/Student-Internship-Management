package com.internmatch.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Concrete production implementation of [FirebaseAuthClient] backed directly
 * by the official Firebase Authentication SDK ([FirebaseAuth]).
 *
 * Technical Requirements Satisfied:
 * - Strongly-typed Firebase SDK APIs (FirebaseAuth, FirebaseUser, AuthResult).
 * - Zero Java reflection.
 * - Asynchronous coroutine task awaiting via kotlinx-coroutines-play-services.
 * - Kept strictly inside the data layer.
 */
class DefaultFirebaseAuthClient(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : FirebaseAuthClient {

    override val isConfigured: Boolean
        get() = true

    override suspend fun signInWithEmailAndPassword(email: String, password: String): TypedAuthResult {
        val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val user: FirebaseUser = authResult.user
            ?: throw IllegalStateException("Firebase user object is null post-authentication.")
        return TypedAuthResult(
            uid = user.uid,
            email = user.email ?: "",
            displayName = user.displayName
        )
    }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String): TypedAuthResult {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user: FirebaseUser = authResult.user
            ?: throw IllegalStateException("Firebase user object is null post-registration.")
        return TypedAuthResult(
            uid = user.uid,
            email = user.email ?: "",
            displayName = user.displayName
        )
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}
