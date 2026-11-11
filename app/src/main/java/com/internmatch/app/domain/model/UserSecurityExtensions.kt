package com.internmatch.app.domain.model

/**
 * Domain extensions providing role-checking capabilities for UI routing
 * and application authorization preconditions.
 *
 * NOTE: UI and client-side role checks provide navigational guidance only.
 * Production security boundaries MUST be enforced by Firestore Security Rules.
 */

val User.isStudent: Boolean
    get() = role == UserRole.STUDENT

val User.isProvider: Boolean
    get() = role == UserRole.PROVIDER

val User.isAdmin: Boolean
    get() = role == UserRole.ADMIN

fun UserRole.canAccessProviderPortal(): Boolean =
    this == UserRole.PROVIDER || this == UserRole.ADMIN

fun UserRole.canAccessAdminConsole(): Boolean =
    this == UserRole.ADMIN
