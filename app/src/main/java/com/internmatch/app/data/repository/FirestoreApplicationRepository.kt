package com.internmatch.app.data.repository

import com.internmatch.app.domain.model.Application
import com.internmatch.app.domain.model.ApplicationException
import com.internmatch.app.domain.model.ApplicationStatus
import com.internmatch.app.domain.model.ApplicationStatusHistory
import com.internmatch.app.domain.model.Certificate
import com.internmatch.app.domain.model.ContactInfo
import com.internmatch.app.domain.model.InternshipStatus
import com.internmatch.app.domain.model.ProficiencyLevel
import com.internmatch.app.domain.model.Project
import com.internmatch.app.domain.model.SkillProficiency
import com.internmatch.app.domain.model.StudentProfileSnapshot
import com.internmatch.app.domain.repository.ApplicationRepository
import com.internmatch.app.domain.repository.InternshipRepository
import com.internmatch.app.domain.repository.StudentProfileRepository
import com.internmatch.app.domain.service.MatchScoreProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Repository implementation for student application submission, tracking, and withdrawal (FR18, FR19, FR21).
 * Strictly enforces authenticated student ownership and transactional data integrity.
 */
class FirestoreApplicationRepository(
    private val applicationClient: FirebaseApplicationClient,
    private val internshipRepository: InternshipRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val matchScoreProvider: MatchScoreProvider,
    private val authenticatedUserIdProvider: () -> String?
) : ApplicationRepository {

    private val _inMemoryApplications = MutableStateFlow<List<Application>>(emptyList())
    private val inMemoryHistoryMap = mutableMapOf<String, MutableList<ApplicationStatusHistory>>()

    private fun getAuthUid(): String? {
        val uid = authenticatedUserIdProvider()
        return if (uid.isNullOrBlank()) null else uid
    }

    override suspend fun submitApplication(
        internshipId: String,
        coverLetter: String,
        resumeUrl: String
    ): Result<Application> {
        val studentId = getAuthUid()
            ?: return Result.failure(ApplicationException.Unauthenticated)

        return try {
            // 1. Fetch & Validate Internship
            val internshipResult = internshipRepository.getInternshipById(internshipId)
            val internship = internshipResult.getOrNull()
                ?: return Result.failure(ApplicationException.InternshipNotFound)

            if (internship.status != InternshipStatus.APPROVED) {
                return Result.failure(ApplicationException.InternshipNotApproved)
            }

            if (internship.applicationDeadline > 0L && System.currentTimeMillis() > internship.applicationDeadline) {
                return Result.failure(ApplicationException.DeadlineExpired)
            }

            // 2. Fetch & Capture Student Profile Snapshot
            val profileResult = studentProfileRepository.getStudentProfile(studentId)
            val profile = profileResult.getOrNull()
                ?: return Result.failure(ApplicationException.ProfileNotFound)

            val profileSnapshot = StudentProfileSnapshot.fromStudentProfile(profile)

            // 3. Calculate Real Match Score
            val matchScore = matchScoreProvider.calculateMatchScore(
                studentSkills = profile.skillProficiencies,
                requiredSkills = internship.requiredSkills
            )

            // 4. Construct Application and History Records
            val appId = "${internshipId}_${studentId}"
            val now = System.currentTimeMillis()

            val application = Application(
                id = appId,
                internshipId = internshipId,
                studentId = studentId,
                companyName = internship.companyName,
                internshipTitle = internship.title,
                status = ApplicationStatus.SUBMITTED,
                submittedAt = now,
                updatedAt = now,
                matchScore = matchScore,
                profileSnapshot = profileSnapshot,
                coverLetter = coverLetter,
                resumeUrl = resumeUrl
            )

            val initialHistory = ApplicationStatusHistory(
                id = "hist_${now}",
                applicationId = appId,
                status = ApplicationStatus.SUBMITTED,
                timestamp = now,
                actorUid = studentId,
                note = "Application submitted by student"
            )

            // 5. Persist to Firestore or In-Memory fallback
            if (applicationClient.isConfigured) {
                val appData = mapApplicationToData(application)
                val historyData = mapHistoryToData(initialHistory)
                applicationClient.submitApplicationTransaction(appId, appData, historyData)
            }

            // Update in-memory cache
            val currentList = _inMemoryApplications.value.toMutableList()
            currentList.removeAll { it.id == appId }
            currentList.add(application)
            _inMemoryApplications.value = currentList

            val historyList = inMemoryHistoryMap.getOrPut(appId) { mutableListOf() }
            historyList.removeAll { it.id == initialHistory.id }
            historyList.add(initialHistory)

            Result.success(application)
        } catch (e: Exception) {
            Result.failure(if (e is ApplicationException) e else ApplicationException.StorageError(e.message ?: "Submission failed", e))
        }
    }

    override suspend fun hasStudentApplied(studentId: String, internshipId: String): Result<Boolean> {
        val authUid = getAuthUid()
            ?: return Result.failure(ApplicationException.Unauthenticated)

        if (authUid != studentId) {
            return Result.failure(ApplicationException.UnauthorizedAccess)
        }

        return try {
            val existsInMemory = _inMemoryApplications.value.any { it.internshipId == internshipId && it.studentId == studentId }
            if (existsInMemory) return Result.success(true)

            if (applicationClient.isConfigured) {
                val existsRemote = applicationClient.hasStudentApplied(studentId, internshipId)
                return Result.success(existsRemote)
            }

            Result.success(false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStudentApplications(studentId: String): Flow<List<Application>> = flow {
        val authUid = getAuthUid()
        if (authUid == null || authUid != studentId) {
            emit(emptyList())
            return@flow
        }

        if (applicationClient.isConfigured) {
            try {
                val rawApps = applicationClient.getStudentApplications(studentId)
                val domainApps = rawApps.mapNotNull { mapDataToApplication(it) }
                    .sortedByDescending { it.submittedAt }
                _inMemoryApplications.value = domainApps
                emit(domainApps)
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                emit(_inMemoryApplications.value.filter { it.studentId == studentId }.sortedByDescending { it.submittedAt })
            }
        } else {
            emit(_inMemoryApplications.value.filter { it.studentId == studentId }.sortedByDescending { it.submittedAt })
        }
    }

    override fun getApplicationById(applicationId: String): Flow<Application?> = flow {
        val authUid = getAuthUid()
        if (authUid == null) {
            emit(null)
            return@flow
        }

        val inMemory = _inMemoryApplications.value.find { it.id == applicationId }
        if (inMemory != null) {
            emit(inMemory)
        }

        if (applicationClient.isConfigured) {
            try {
                val rawApp = applicationClient.getApplicationById(applicationId)
                val app = rawApp?.let { mapDataToApplication(it) }
                if (app != null && app.studentId == authUid) {
                    val currentList = _inMemoryApplications.value.toMutableList()
                    currentList.removeAll { it.id == app.id }
                    currentList.add(app)
                    _inMemoryApplications.value = currentList
                    if (inMemory == null) {
                        emit(app)
                    }
                } else if (inMemory == null) {
                    emit(null)
                }
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                if (inMemory == null) emit(null)
            }
        } else if (inMemory == null) {
            emit(null)
        }
    }

    override fun getApplicationHistory(applicationId: String): Flow<List<ApplicationStatusHistory>> = flow {
        val authUid = getAuthUid()
        if (authUid == null) {
            emit(emptyList())
            return@flow
        }

        val localHistory = inMemoryHistoryMap[applicationId]?.sortedBy { it.timestamp } ?: emptyList()
        if (localHistory.isNotEmpty()) {
            emit(localHistory)
        }

        if (applicationClient.isConfigured) {
            try {
                val rawHistory = applicationClient.getApplicationHistory(applicationId)
                val historyList = rawHistory.mapNotNull { mapDataToHistory(it) }
                    .sortedBy { it.timestamp }
                inMemoryHistoryMap[applicationId] = historyList.toMutableList()
                if (localHistory.isEmpty()) {
                    emit(historyList)
                }
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                if (localHistory.isEmpty()) emit(emptyList())
            }
        } else if (localHistory.isEmpty()) {
            emit(emptyList())
        }
    }

    override suspend fun withdrawApplication(applicationId: String): Result<Unit> {
        val studentId = getAuthUid()
            ?: return Result.failure(ApplicationException.Unauthenticated)

        return try {
            var currentApp: Application? = _inMemoryApplications.value.find { it.id == applicationId }
            if (currentApp == null && applicationClient.isConfigured) {
                val raw = applicationClient.getApplicationById(applicationId)
                currentApp = raw?.let { mapDataToApplication(it) }
            }

            val app = currentApp ?: return Result.failure(ApplicationException.ApplicationNotFound)
            if (app.studentId != studentId) {
                return Result.failure(ApplicationException.UnauthorizedAccess)
            }

            if (!app.isWithdrawable) {
                return Result.failure(ApplicationException.NotEligibleForWithdrawal)
            }

            val now = System.currentTimeMillis()
            val historyEvent = ApplicationStatusHistory(
                id = "hist_${now}",
                applicationId = applicationId,
                status = ApplicationStatus.WITHDRAWN,
                timestamp = now,
                actorUid = studentId,
                note = "Withdrawn by student"
            )

            if (applicationClient.isConfigured) {
                val historyData = mapHistoryToData(historyEvent)
                applicationClient.withdrawApplicationTransaction(applicationId, studentId, historyData)
            }

            // Update in-memory
            val updatedApp = app.copy(status = ApplicationStatus.WITHDRAWN, updatedAt = now)
            val currentList = _inMemoryApplications.value.toMutableList()
            currentList.removeAll { it.id == applicationId }
            currentList.add(updatedApp)
            _inMemoryApplications.value = currentList

            val historyList = inMemoryHistoryMap.getOrPut(applicationId) { mutableListOf() }
            historyList.add(historyEvent)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(if (e is ApplicationException) e else ApplicationException.StorageError(e.message ?: "Withdrawal failed", e))
        }
    }

    // Mapping helper methods
    private fun mapApplicationToData(app: Application): Map<String, Any?> {
        return mapOf(
            "id" to app.id,
            "internshipId" to app.internshipId,
            "studentId" to app.studentId,
            "companyName" to app.companyName,
            "internshipTitle" to app.internshipTitle,
            "status" to app.status.name,
            "submittedAt" to app.submittedAt,
            "updatedAt" to app.updatedAt,
            "matchScore" to app.matchScore,
            "coverLetter" to app.coverLetter,
            "resumeUrl" to app.resumeUrl,
            "profileSnapshot" to app.profileSnapshot?.let { mapSnapshotToData(it) }
        )
    }

    private fun mapSnapshotToData(snapshot: StudentProfileSnapshot): Map<String, Any?> {
        return mapOf(
            "studentId" to snapshot.studentId,
            "registrationNumber" to snapshot.registrationNumber,
            "university" to snapshot.university,
            "degreeProgram" to snapshot.degreeProgram,
            "academicYear" to snapshot.academicYear,
            "semester" to snapshot.semester,
            "gpa" to snapshot.gpa,
            "bio" to snapshot.bio,
            "contactDetails" to snapshot.contactDetails?.let {
                mapOf(
                    "phone" to it.phone,
                    "linkedinUrl" to it.linkedinUrl,
                    "githubUrl" to it.githubUrl,
                    "portfolioUrl" to it.portfolioUrl
                )
            },
            "skillProficiencies" to snapshot.skillProficiencies.map {
                mapOf("skillId" to it.skillId, "level" to it.level.name)
            },
            "projects" to snapshot.projects.map {
                mapOf("title" to it.title, "description" to it.description, "projectUrl" to it.projectUrl)
            },
            "certificates" to snapshot.certificates.map {
                mapOf("title" to it.title, "issuer" to it.issuer, "issueDate" to it.issueDate)
            }
        )
    }

    private fun mapHistoryToData(history: ApplicationStatusHistory): Map<String, Any?> {
        return mapOf(
            "id" to history.id,
            "applicationId" to history.applicationId,
            "status" to history.status.name,
            "timestamp" to history.timestamp,
            "actorUid" to history.actorUid,
            "note" to history.note
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDataToApplication(data: Map<String, Any?>): Application? {
        val id = data["id"] as? String ?: return null
        val internshipId = data["internshipId"] as? String ?: return null
        val studentId = data["studentId"] as? String ?: return null
        val companyName = data["companyName"] as? String ?: ""
        val internshipTitle = data["internshipTitle"] as? String ?: ""
        val rawStatus = data["status"] as? String ?: "SUBMITTED"
        val status = try { ApplicationStatus.valueOf(rawStatus) } catch (_: Exception) { ApplicationStatus.SUBMITTED }
        val submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: 0L
        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
        val matchScore = (data["matchScore"] as? Number)?.toDouble()
        val coverLetter = data["coverLetter"] as? String ?: ""
        val resumeUrl = data["resumeUrl"] as? String ?: ""

        val rawSnapshot = data["profileSnapshot"] as? Map<String, Any?>
        val profileSnapshot = rawSnapshot?.let { mapDataToSnapshot(it) }

        return Application(
            id = id,
            internshipId = internshipId,
            studentId = studentId,
            companyName = companyName,
            internshipTitle = internshipTitle,
            status = status,
            submittedAt = submittedAt,
            updatedAt = updatedAt,
            matchScore = matchScore,
            profileSnapshot = profileSnapshot,
            coverLetter = coverLetter,
            resumeUrl = resumeUrl
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDataToSnapshot(map: Map<String, Any?>): StudentProfileSnapshot {
        val studentId = map["studentId"] as? String ?: ""
        val registrationNumber = map["registrationNumber"] as? String ?: ""
        val university = map["university"] as? String ?: ""
        val degreeProgram = map["degreeProgram"] as? String ?: ""
        val academicYear = (map["academicYear"] as? Number)?.toInt() ?: 1
        val semester = (map["semester"] as? Number)?.toInt() ?: 1
        val gpa = (map["gpa"] as? Number)?.toDouble()
        val bio = map["bio"] as? String ?: ""

        val contactMap = map["contactDetails"] as? Map<String, Any?>
        val contactDetails = contactMap?.let {
            ContactInfo(
                phone = it["phone"] as? String ?: "",
                linkedinUrl = it["linkedinUrl"] as? String ?: "",
                githubUrl = it["githubUrl"] as? String ?: "",
                portfolioUrl = it["portfolioUrl"] as? String ?: ""
            )
        }

        val rawSkills = map["skillProficiencies"] as? List<Map<String, Any?>> ?: emptyList()
        val skillProficiencies = rawSkills.mapNotNull { sMap ->
            val sId = sMap["skillId"] as? String ?: return@mapNotNull null
            val lStr = sMap["level"] as? String ?: "BEGINNER"
            val level = try { ProficiencyLevel.valueOf(lStr) } catch (_: Exception) { ProficiencyLevel.BEGINNER }
            SkillProficiency(skillId = sId, level = level)
        }

        val rawProjects = map["projects"] as? List<Map<String, Any?>> ?: emptyList()
        val projects = rawProjects.mapNotNull { pMap ->
            val title = pMap["title"] as? String ?: return@mapNotNull null
            val desc = pMap["description"] as? String ?: ""
            val url = pMap["projectUrl"] as? String ?: ""
            Project(title = title, description = desc, projectUrl = url)
        }

        val rawCerts = map["certificates"] as? List<Map<String, Any?>> ?: emptyList()
        val certificates = rawCerts.mapNotNull { cMap ->
            val title = cMap["title"] as? String ?: return@mapNotNull null
            val issuer = cMap["issuer"] as? String ?: ""
            val issueDate = cMap["issueDate"] as? String ?: ""
            Certificate(title = title, issuer = issuer, issueDate = issueDate)
        }

        return StudentProfileSnapshot(
            studentId = studentId,
            registrationNumber = registrationNumber,
            university = university,
            degreeProgram = degreeProgram,
            academicYear = academicYear,
            semester = semester,
            gpa = gpa,
            bio = bio,
            contactDetails = contactDetails,
            projects = projects,
            certificates = certificates,
            skillProficiencies = skillProficiencies
        )
    }

    private fun mapDataToHistory(data: Map<String, Any?>): ApplicationStatusHistory? {
        val id = data["id"] as? String ?: return null
        val applicationId = data["applicationId"] as? String ?: ""
        val rawStatus = data["status"] as? String ?: "SUBMITTED"
        val status = try { ApplicationStatus.valueOf(rawStatus) } catch (_: Exception) { ApplicationStatus.SUBMITTED }
        val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val actorUid = data["actorUid"] as? String ?: ""
        val note = data["note"] as? String

        return ApplicationStatusHistory(
            id = id,
            applicationId = applicationId,
            status = status,
            timestamp = timestamp,
            actorUid = actorUid,
            note = note
        )
    }
}
