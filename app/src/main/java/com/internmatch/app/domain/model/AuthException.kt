package com.internmatch.app.domain.model

/**
 * Domain-specific authentication exceptions mapping Firebase Auth error codes
 * into application-level error representations without leaking SDK types.
 */
sealed class AuthException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    class InvalidEmail(
        message: String = "The email address provided is invalid."
    ) : AuthException(message)

    class WeakPassword(
        message: String = "The password provided is too weak. Please use a stronger password."
    ) : AuthException(message)

    class InvalidCredentials(
        message: String = "Invalid email or password. Please check your credentials."
    ) : AuthException(message)

    class EmailAlreadyInUse(
        message: String = "An account already exists with this email address."
    ) : AuthException(message)

    class UserNotFound(
        message: String = "No registered user account was found with this email."
    ) : AuthException(message)

    class UserDisabled(
        message: String = "This user account has been disabled by an administrator."
    ) : AuthException(message)

    class TooManyRequests(
        message: String = "Too many unsuccessful attempts. Please try again later."
    ) : AuthException(message)

    class NetworkError(
        message: String = "A network error occurred. Please check your internet connection."
    ) : AuthException(message)

    class ServiceUnavailable(
        message: String = "Authentication service is unavailable or Firebase is unconfigured."
    ) : AuthException(message)

    class Unknown(
        message: String = "An unexpected authentication error occurred.",
        cause: Throwable? = null
    ) : AuthException(message, cause)
}
