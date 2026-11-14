package com.internmatch.app.domain.model

/**
 * Domain exceptions for application management workflows (FR18, FR19, FR21).
 */
sealed class ApplicationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    object Unauthenticated : ApplicationException("User must be authenticated to perform application actions.")
    object AlreadyApplied : ApplicationException("You have already submitted an application for this internship.")
    object InternshipNotFound : ApplicationException("The specified internship could not be found.")
    object InternshipNotApproved : ApplicationException("Applications can only be submitted for approved internships.")
    object InternshipClosed : ApplicationException("This internship is closed and no longer accepting applications.")
    object DeadlineExpired : ApplicationException("The application deadline for this internship has passed.")
    object ProfileNotFound : ApplicationException("Student profile was not found. Please complete your profile before applying.")
    object NotEligibleForWithdrawal : ApplicationException("Application cannot be withdrawn after a final decision (Accepted/Rejected).")
    object ApplicationNotFound : ApplicationException("Application not found.")
    object UnauthorizedAccess : ApplicationException("You do not have permission to access or modify this application.")
    class StorageError(message: String, cause: Throwable? = null) : ApplicationException(message, cause)
}
