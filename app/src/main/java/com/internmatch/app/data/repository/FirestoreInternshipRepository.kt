package com.internmatch.app.data.repository

import com.internmatch.app.domain.model.Internship
import com.internmatch.app.domain.model.InternshipStatus
import com.internmatch.app.domain.model.RequiredSkill
import com.internmatch.app.domain.repository.InternshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository implementation for managing and querying internship listings.
 * Enforces status = APPROVED filter and client-side in-memory search across keyword,
 * domain, district, and work mode.
 */
class FirestoreInternshipRepository(
    private val internshipClient: FirebaseInternshipClient
) : InternshipRepository {

    private val _approvedInternships = MutableStateFlow<List<Internship>>(emptyList())
    override val approvedInternships: Flow<List<Internship>> = _approvedInternships.asStateFlow()

    override suspend fun getApprovedInternships(): Result<List<Internship>> {
        return try {
            if (!internshipClient.isConfigured) {
                return Result.success(_approvedInternships.value)
            }
            val rawList = internshipClient.getApprovedInternships(limit = 200)
            val domainList = rawList.mapNotNull { mapToInternship(it) }
                .filter { it.status == InternshipStatus.APPROVED }

            _approvedInternships.value = domainList
            Result.success(domainList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInternshipById(id: String): Result<Internship?> {
        return try {
            val currentList = if (_approvedInternships.value.isEmpty()) {
                val fetchResult = getApprovedInternships()
                fetchResult.getOrDefault(emptyList())
            } else {
                _approvedInternships.value
            }
            val item = currentList.find { it.id == id }
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchInternships(
        query: String,
        domain: String,
        district: String,
        workMode: String
    ): Result<List<Internship>> {
        return try {
            val list = if (_approvedInternships.value.isEmpty()) {
                val fetchResult = getApprovedInternships()
                if (fetchResult.isFailure) {
                    return fetchResult
                }
                fetchResult.getOrDefault(emptyList())
            } else {
                _approvedInternships.value
            }

            val filtered = list.filter { internship ->
                // Must be APPROVED
                if (internship.status != InternshipStatus.APPROVED) return@filter false

                // Keyword match (title, companyName, description)
                val matchesQuery = query.isBlank() ||
                    internship.title.contains(query, ignoreCase = true) ||
                    internship.companyName.contains(query, ignoreCase = true) ||
                    internship.description.contains(query, ignoreCase = true)

                // Domain filter
                val matchesDomain = domain.isBlank() ||
                    internship.domain.equals(domain, ignoreCase = true) ||
                    internship.domain.contains(domain, ignoreCase = true)

                // District filter
                val matchesDistrict = district.isBlank() ||
                    internship.district.equals(district, ignoreCase = true) ||
                    internship.district.contains(district, ignoreCase = true)

                // Work mode filter
                val matchesWorkMode = workMode.isBlank() ||
                    internship.workMode.equals(workMode, ignoreCase = true) ||
                    internship.workMode.contains(workMode, ignoreCase = true)

                matchesQuery && matchesDomain && matchesDistrict && matchesWorkMode
            }

            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToInternship(data: Map<String, Any?>): Internship? {
        val id = data["id"] as? String ?: return null
        val title = data["title"] as? String ?: ""
        val companyName = data["companyName"] as? String ?: ""
        val providerId = data["providerId"] as? String ?: ""
        val description = data["description"] as? String ?: ""
        val location = data["location"] as? String ?: ""
        val employmentType = data["employmentType"] as? String ?: ""
        val domain = data["domain"] as? String ?: ""
        val district = data["district"] as? String ?: ""
        val workMode = data["workMode"] as? String ?: ""

        val rawStatus = data["status"] as? String ?: "APPROVED"
        val status = try {
            InternshipStatus.valueOf(rawStatus.uppercase())
        } catch (_: Exception) {
            InternshipStatus.APPROVED
        }

        val rawSkills = data["requiredSkills"] as? List<Map<String, Any?>> ?: emptyList()
        val requiredSkills = rawSkills.mapNotNull { skillMap ->
            val skillId = skillMap["skillId"] as? String ?: return@mapNotNull null
            val weight = (skillMap["weight"] as? Number)?.toDouble() ?: 1.0
            RequiredSkill(skillId = skillId, weight = weight)
        }

        val applicationDeadline = (data["applicationDeadline"] as? Number)?.toLong() ?: 0L
        val createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L

        return Internship(
            id = id,
            providerId = providerId,
            companyName = companyName,
            title = title,
            description = description,
            location = location,
            employmentType = employmentType,
            domain = domain,
            district = district,
            workMode = workMode,
            requiredSkills = requiredSkills,
            status = status,
            applicationDeadline = applicationDeadline,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
