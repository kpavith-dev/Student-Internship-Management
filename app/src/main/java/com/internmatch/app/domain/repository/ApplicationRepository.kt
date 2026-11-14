package com.internmatch.app.domain.repository

import com.internmatch.app.domain.model.Application
import com.internmatch.app.domain.model.ApplicationStatusHistory
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing student internship applications (FR18, FR19, FR21).
 */
interface ApplicationRepository {
    /**
     * Submits an application for an internship (FR18).
     * Atomically captures student profile snapshot and match score.
     */
    suspend fun submitApplication(
        internshipId: String,
        coverLetter: String = "",
        resumeUrl: String = ""
    ): Result<Application>

    /**
     * Checks whether the specified student has already applied to an internship.
     */
    suspend fun hasStudentApplied(studentId: String, internshipId: String): Result<Boolean>

    /**
     * Streams applications belonging to the specified student (FR19).
     */
    fun getStudentApplications(studentId: String): Flow<List<Application>>

    /**
     * Streams a single application by its unique ID.
     */
    fun getApplicationById(applicationId: String): Flow<Application?>

    /**
     * Streams the status transition audit history for an application (FR19).
     */
    fun getApplicationHistory(applicationId: String): Flow<List<ApplicationStatusHistory>>

    /**
     * Withdraws an active application submitted by the student (FR21).
     */
    suspend fun withdrawApplication(applicationId: String): Result<Unit>
}
