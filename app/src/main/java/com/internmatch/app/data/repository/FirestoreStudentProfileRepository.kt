package com.internmatch.app.data.repository

import com.internmatch.app.domain.model.Certificate
import com.internmatch.app.domain.model.ContactInfo
import com.internmatch.app.domain.model.ProficiencyLevel
import com.internmatch.app.domain.model.Project
import com.internmatch.app.domain.model.SkillProficiency
import com.internmatch.app.domain.model.StudentProfile
import com.internmatch.app.domain.repository.StudentProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production implementation of [StudentProfileRepository] backed by Cloud Firestore.
 *
 * Features & Security Boundaries:
 * - MANDATORY production [FirebaseStudentProfileClient] dependency.
 * - MANDATORY UID ownership verification via [authenticatedUserIdProvider].
 * - Atomic Create vs Merge Update semantics via client transactions.
 * - Explicit configuration failure reporting when Firebase is unconfigured.
 * - Strict mapping validation: Throws [IllegalArgumentException] for corrupted required fields,
 *   out-of-bounds numbers, decimal year/semester values, invalid list structures, or malformed level enum values.
 * - Preserves coroutine [CancellationException] propagation.
 */
class FirestoreStudentProfileRepository(
    private val profileClient: FirebaseStudentProfileClient,
    private val authenticatedUserIdProvider: () -> String?
) : StudentProfileRepository {

    private val _currentStudentProfile = MutableStateFlow<StudentProfile?>(null)
    override val currentStudentProfile: Flow<StudentProfile?> = _currentStudentProfile.asStateFlow()

    val isConfigured: Boolean
        get() = profileClient.isConfigured

    private fun validateOwnership(targetStudentId: String): SecurityException? {
        val currentUid = authenticatedUserIdProvider.invoke()
            ?: return SecurityException("Access denied. User is unauthenticated.")

        if (currentUid != targetStudentId) {
            return SecurityException("Access denied. Authenticated UID '$currentUid' cannot read or modify profile for '$targetStudentId'.")
        }
        return null
    }

    override suspend fun getStudentProfile(studentId: String): Result<StudentProfile?> {
        if (studentId.isBlank()) {
            return Result.failure(IllegalArgumentException("Student UID cannot be blank."))
        }

        validateOwnership(studentId)?.let { securityError ->
            return Result.failure(securityError)
        }

        if (!isConfigured) {
            return Result.failure(IllegalStateException("Firebase Firestore is unconfigured or unavailable."))
        }

        return try {
            val data = profileClient.getProfile(studentId)
            val profile = data?.let { mapDataToStudentProfile(studentId, it) }
            _currentStudentProfile.value = profile
            Result.success(profile)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun createStudentProfile(profile: StudentProfile): Result<StudentProfile> {
        if (profile.studentId.isBlank()) {
            return Result.failure(IllegalArgumentException("Student ID cannot be blank."))
        }
        if (profile.university.isBlank()) {
            return Result.failure(IllegalArgumentException("University name cannot be blank."))
        }
        if (profile.degreeProgram.isBlank()) {
            return Result.failure(IllegalArgumentException("Degree programme cannot be blank."))
        }
        if (profile.registrationNumber.isBlank()) {
            return Result.failure(IllegalArgumentException("Registration number cannot be blank."))
        }

        validateOwnership(profile.studentId)?.let { securityError ->
            return Result.failure(securityError)
        }

        if (!isConfigured) {
            return Result.failure(IllegalStateException("Firebase Firestore is unconfigured or unavailable."))
        }

        return try {
            val data = mapStudentProfileToData(profile)
            profileClient.createProfile(profile.studentId, data)
            _currentStudentProfile.value = profile
            Result.success(profile)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun updateStudentProfile(profile: StudentProfile): Result<StudentProfile> {
        if (profile.studentId.isBlank()) {
            return Result.failure(IllegalArgumentException("Student ID cannot be blank."))
        }
        if (profile.university.isBlank()) {
            return Result.failure(IllegalArgumentException("University name cannot be blank."))
        }
        if (profile.degreeProgram.isBlank()) {
            return Result.failure(IllegalArgumentException("Degree programme cannot be blank."))
        }
        if (profile.registrationNumber.isBlank()) {
            return Result.failure(IllegalArgumentException("Registration number cannot be blank."))
        }

        validateOwnership(profile.studentId)?.let { securityError ->
            return Result.failure(securityError)
        }

        if (!isConfigured) {
            return Result.failure(IllegalStateException("Firebase Firestore is unconfigured or unavailable."))
        }

        return try {
            val data = mapStudentProfileToData(profile)
            profileClient.updateProfile(profile.studentId, data)
            _currentStudentProfile.value = profile
            Result.success(profile)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun updateSkillProficiencies(
        studentId: String,
        skillProficiencies: List<SkillProficiency>
    ): Result<List<SkillProficiency>> {
        if (studentId.isBlank()) {
            return Result.failure(IllegalArgumentException("Student ID cannot be blank."))
        }

        validateOwnership(studentId)?.let { securityError ->
            return Result.failure(securityError)
        }

        if (!isConfigured) {
            return Result.failure(IllegalStateException("Firebase Firestore is unconfigured or unavailable."))
        }

        return try {
            val data = skillProficiencies.map { mapSkillProficiencyToData(it) }
            profileClient.updateSkillProficiencies(studentId, data)
            val currentProf = _currentStudentProfile.value ?: StudentProfile(studentId = studentId)
            _currentStudentProfile.value = currentProf.copy(skillProficiencies = skillProficiencies)
            Result.success(skillProficiencies)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun mapDataToStudentProfile(studentId: String, data: Map<String, Any?>): StudentProfile {
        val regNum = data["registrationNumber"] as? String
        if (regNum.isNullOrBlank()) {
            throw IllegalArgumentException("Malformed or missing required field 'registrationNumber': Non-blank String expected.")
        }

        val university = data["university"] as? String
        if (university.isNullOrBlank()) {
            throw IllegalArgumentException("Malformed or missing required field 'university': Non-blank String expected.")
        }

        val degreeProgram = data["degreeProgram"] as? String
        if (degreeProgram.isNullOrBlank()) {
            throw IllegalArgumentException("Malformed or missing required field 'degreeProgram': Non-blank String expected.")
        }

        val academicYearObj = data["academicYear"]
            ?: throw IllegalArgumentException("Missing required field 'academicYear'.")
        val academicYearNumber = academicYearObj as? Number
            ?: throw IllegalArgumentException("Malformed field 'academicYear': Integer number expected.")
        if (academicYearNumber.toDouble() != academicYearNumber.toInt().toDouble()) {
            throw IllegalArgumentException("Malformed field 'academicYear': Integer expected, got decimal value $academicYearNumber.")
        }
        val academicYear = academicYearNumber.toInt()
        if (academicYear !in 1..6) {
            throw IllegalArgumentException("Malformed field 'academicYear': Value $academicYear out of valid range 1..6.")
        }

        val semesterObj = data["semester"]
            ?: throw IllegalArgumentException("Missing required field 'semester'.")
        val semesterNumber = semesterObj as? Number
            ?: throw IllegalArgumentException("Malformed field 'semester': Integer number expected.")
        if (semesterNumber.toDouble() != semesterNumber.toInt().toDouble()) {
            throw IllegalArgumentException("Malformed field 'semester': Integer expected, got decimal value $semesterNumber.")
        }
        val semester = semesterNumber.toInt()
        if (semester !in 1..12) {
            throw IllegalArgumentException("Malformed field 'semester': Value $semester out of valid range 1..12.")
        }

        val gpaObj = data["gpa"]
        val gpaNumber = gpaObj?.let {
            it as? Number ?: throw IllegalArgumentException("Malformed field 'gpa': Numeric Double value expected.")
        }
        val gpa = gpaNumber?.toDouble()
        if (gpa != null && gpa !in 0.0..4.0) {
            throw IllegalArgumentException("Malformed field 'gpa': Value $gpa out of valid range 0.0..4.0.")
        }

        val bio = data["bio"] as? String ?: ""

        val contactMapObj = data["contactDetails"]
        val contactInfo = if (contactMapObj != null) {
            val contactMap = contactMapObj as? Map<String, Any?>
                ?: throw IllegalArgumentException("Malformed field 'contactDetails': Map structure expected.")

            val phone = contactMap["phone"]?.let {
                it as? String ?: throw IllegalArgumentException("Malformed field 'phone' in contactDetails: String expected.")
            } ?: ""

            val linkedin = contactMap["linkedinUrl"]?.let {
                it as? String ?: throw IllegalArgumentException("Malformed field 'linkedinUrl' in contactDetails: String expected.")
            } ?: ""

            val github = contactMap["githubUrl"]?.let {
                it as? String ?: throw IllegalArgumentException("Malformed field 'githubUrl' in contactDetails: String expected.")
            } ?: ""

            val portfolio = contactMap["portfolioUrl"]?.let {
                it as? String ?: throw IllegalArgumentException("Malformed field 'portfolioUrl' in contactDetails: String expected.")
            } ?: ""

            ContactInfo(phone = phone, linkedinUrl = linkedin, githubUrl = github, portfolioUrl = portfolio)
        } else null

        val projectsObj = data["projects"]
        val projectsList = if (projectsObj != null) {
            val rawList = projectsObj as? List<Any?>
                ?: throw IllegalArgumentException("Malformed field 'projects': List expected.")
            rawList.map { item ->
                val p = item as? Map<String, Any?>
                    ?: throw IllegalArgumentException("Malformed project entry: Map structure expected.")
                val title = p["title"] as? String
                if (title.isNullOrBlank()) {
                    throw IllegalArgumentException("Malformed or missing required field 'title' in project entry.")
                }
                val desc = p["description"]?.let {
                    it as? String ?: throw IllegalArgumentException("Malformed field 'description' in project entry: String expected.")
                } ?: ""
                val url = p["projectUrl"]?.let {
                    it as? String ?: throw IllegalArgumentException("Malformed field 'projectUrl' in project entry: String expected.")
                } ?: ""
                Project(title = title, description = desc, projectUrl = url)
            }
        } else emptyList()

        val certsObj = data["certificates"]
        val certsList = if (certsObj != null) {
            val rawList = certsObj as? List<Any?>
                ?: throw IllegalArgumentException("Malformed field 'certificates': List expected.")
            rawList.map { item ->
                val c = item as? Map<String, Any?>
                    ?: throw IllegalArgumentException("Malformed certificate entry: Map structure expected.")
                val title = c["title"] as? String
                if (title.isNullOrBlank()) {
                    throw IllegalArgumentException("Malformed or missing required field 'title' in certificate entry.")
                }
                val issuer = c["issuer"]?.let {
                    it as? String ?: throw IllegalArgumentException("Malformed field 'issuer' in certificate entry: String expected.")
                } ?: ""
                val date = c["issueDate"]?.let {
                    it as? String ?: throw IllegalArgumentException("Malformed field 'issueDate' in certificate entry: String expected.")
                } ?: ""
                Certificate(title = title, issuer = issuer, issueDate = date)
            }
        } else emptyList()

        val skillsObj = data["skillProficiencies"]
        val skillsList = if (skillsObj != null) {
            val rawList = skillsObj as? List<Any?>
                ?: throw IllegalArgumentException("Malformed field 'skillProficiencies': List expected.")
            rawList.map { item ->
                val s = item as? Map<String, Any?>
                    ?: throw IllegalArgumentException("Malformed skillProficiencies entry: Map structure expected.")
                val skillId = s["skillId"] as? String
                if (skillId.isNullOrBlank()) {
                    throw IllegalArgumentException("Malformed or missing required field 'skillId' in skillProficiencies entry.")
                }
                val levelStr = s["level"] as? String
                    ?: throw IllegalArgumentException("Malformed field 'level' in skillProficiencies entry: String expected.")
                val level = try {
                    ProficiencyLevel.valueOf(levelStr)
                } catch (_: Exception) {
                    throw IllegalArgumentException("Invalid proficiency level '$levelStr' for skill '$skillId'.")
                }
                SkillProficiency(skillId = skillId, level = level)
            }
        } else emptyList()

        return StudentProfile(
            studentId = studentId,
            registrationNumber = regNum,
            university = university,
            degreeProgram = degreeProgram,
            academicYear = academicYear,
            semester = semester,
            gpa = gpa,
            bio = bio,
            contactDetails = contactInfo,
            projects = projectsList,
            certificates = certsList,
            skillProficiencies = skillsList
        )
    }

    fun mapStudentProfileToData(profile: StudentProfile): Map<String, Any?> {
        return mapOf(
            "studentId" to profile.studentId,
            "registrationNumber" to profile.registrationNumber,
            "university" to profile.university,
            "degreeProgram" to profile.degreeProgram,
            "academicYear" to profile.academicYear,
            "semester" to profile.semester,
            "gpa" to profile.gpa,
            "bio" to profile.bio,
            "contactDetails" to profile.contactDetails?.let {
                mapOf(
                    "phone" to it.phone,
                    "linkedinUrl" to it.linkedinUrl,
                    "githubUrl" to it.githubUrl,
                    "portfolioUrl" to it.portfolioUrl
                )
            },
            "projects" to profile.projects.map { p ->
                mapOf(
                    "title" to p.title,
                    "description" to p.description,
                    "projectUrl" to p.projectUrl
                )
            },
            "certificates" to profile.certificates.map { c ->
                mapOf(
                    "title" to c.title,
                    "issuer" to c.issuer,
                    "issueDate" to c.issueDate
                )
            },
            "skillProficiencies" to profile.skillProficiencies.map { mapSkillProficiencyToData(it) }
        )
    }

    private fun mapSkillProficiencyToData(sp: SkillProficiency): Map<String, Any?> {
        return mapOf(
            "skillId" to sp.skillId,
            "level" to sp.level.name
        )
    }
}
