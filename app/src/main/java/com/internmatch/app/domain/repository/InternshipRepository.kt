package com.internmatch.app.domain.repository

import com.internmatch.app.domain.model.Internship
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for searching, filtering, and retrieving approved internships.
 * Per Accepted Decision 3 & Section 4:
 * Only internships with status = APPROVED are accessible to student queries.
 */
interface InternshipRepository {
    /**
     * Flow observing the active list of approved internships.
     */
    val approvedInternships: Flow<List<Internship>>

    /**
     * Retrieves all approved internships asynchronously.
     */
    suspend fun getApprovedInternships(): Result<List<Internship>>

    /**
     * Retrieves a specific approved internship by ID.
     */
    suspend fun getInternshipById(id: String): Result<Internship?>

    /**
     * Searches and filters approved internships in-memory or by query.
     */
    suspend fun searchInternships(
        query: String = "",
        domain: String = "",
        district: String = "",
        workMode: String = ""
    ): Result<List<Internship>>
}
