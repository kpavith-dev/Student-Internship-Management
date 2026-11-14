package com.internmatch.app.data.repository

import com.internmatch.app.domain.model.ApplicationException
import com.internmatch.app.domain.model.ApplicationStatus
import com.internmatch.app.domain.model.Internship
import com.internmatch.app.domain.model.InternshipStatus
import com.internmatch.app.domain.model.ProficiencyLevel
import com.internmatch.app.domain.model.RequiredSkill
import com.internmatch.app.domain.model.SkillProficiency
import com.internmatch.app.domain.model.StudentProfile
import com.internmatch.app.domain.repository.InternshipRepository
import com.internmatch.app.domain.repository.StudentProfileRepository
import com.internmatch.app.domain.service.DefaultMatchScoreProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirestoreApplicationRepositoryTest {

    private lateinit var repository: FirestoreApplicationRepository
    private lateinit var fakeClient: FakeFirebaseApplicationClient
    private lateinit var fakeInternshipRepository: FakeInternshipRepository
    private lateinit var fakeStudentProfileRepository: FakeStudentProfileRepository
    private var currentAuthUid: String? = "student_123"

    @Before
    fun setUp() {
        fakeClient = FakeFirebaseApplicationClient()
        fakeInternshipRepository = FakeInternshipRepository()
        fakeStudentProfileRepository = FakeStudentProfileRepository()

        repository = FirestoreApplicationRepository(
            applicationClient = fakeClient,
            internshipRepository = fakeInternshipRepository,
            studentProfileRepository = fakeStudentProfileRepository,
            matchScoreProvider = DefaultMatchScoreProvider(),
            authenticatedUserIdProvider = { currentAuthUid }
        )

        // Setup mock approved internship
        fakeInternshipRepository.addInternship(
            Internship(
                id = "internship_1",
                companyName = "Wso2",
                title = "Software Engineer Intern",
                status = InternshipStatus.APPROVED,
                requiredSkills = listOf(
                    RequiredSkill(skillId = "kotlin", weight = 1.0),
                    RequiredSkill(skillId = "android", weight = 1.0)
                ),
                applicationDeadline = System.currentTimeMillis() + 86400000L
            )
        )

        // Setup mock unapproved internship
        fakeInternshipRepository.addInternship(
            Internship(
                id = "internship_unapproved",
                companyName = "Draft Tech",
                title = "Draft Intern",
                status = InternshipStatus.DRAFT
            )
        )

        // Setup mock expired internship
        fakeInternshipRepository.addInternship(
            Internship(
                id = "internship_expired",
                companyName = "Expired Tech",
                title = "Past Intern",
                status = InternshipStatus.APPROVED,
                applicationDeadline = System.currentTimeMillis() - 86400000L
            )
        )

        // Setup student profile
        fakeStudentProfileRepository.setProfile(
            StudentProfile(
                studentId = "student_123",
                university = "University of Jaffna",
                degreeProgram = "Computer Science",
                skillProficiencies = listOf(
                    SkillProficiency(skillId = "kotlin", level = ProficiencyLevel.ADVANCED)
                )
            )
        )
    }

    @Test
    fun `submitApplication succeeds and captures profile snapshot and match score`() = runBlocking {
        val result = repository.submitApplication("internship_1", coverLetter = "I love Kotlin")

        assertTrue(result.isSuccess)
        val app = result.getOrThrow()
        assertEquals("internship_1_student_123", app.id)
        assertEquals("internship_1", app.internshipId)
        assertEquals("student_123", app.studentId)
        assertEquals("Wso2", app.companyName)
        assertEquals("Software Engineer Intern", app.internshipTitle)
        assertEquals(ApplicationStatus.SUBMITTED, app.status)
        assertEquals(50.0, app.matchScore!!, 0.01) // 1 of 2 skills matched (50%)
        assertNotNull(app.profileSnapshot)
        assertEquals("University of Jaffna", app.profileSnapshot?.university)

        // Verify initial history
        val history = repository.getApplicationHistory(app.id).first()
        assertEquals(1, history.size)
        assertEquals(ApplicationStatus.SUBMITTED, history[0].status)
        assertEquals("student_123", history[0].actorUid)
    }

    @Test
    fun `submitApplication fails when unauthenticated`() = runBlocking {
        currentAuthUid = null
        val result = repository.submitApplication("internship_1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApplicationException.Unauthenticated)
    }

    @Test
    fun `submitApplication fails when duplicate application submitted`() = runBlocking {
        repository.submitApplication("internship_1")
        val duplicateResult = repository.submitApplication("internship_1")

        assertTrue(duplicateResult.isFailure)
        assertTrue(duplicateResult.exceptionOrNull() is ApplicationException.AlreadyApplied)
    }

    @Test
    fun `submitApplication fails when internship is not approved`() = runBlocking {
        val result = repository.submitApplication("internship_unapproved")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApplicationException.InternshipNotApproved)
    }

    @Test
    fun `submitApplication fails when application deadline has expired`() = runBlocking {
        val result = repository.submitApplication("internship_expired")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApplicationException.DeadlineExpired)
    }

    @Test
    fun `getStudentApplications returns only applications belonging to authenticated student`() = runBlocking {
        repository.submitApplication("internship_1")

        val studentApps = repository.getStudentApplications("student_123").first()
        assertEquals(1, studentApps.size)

        val unauthorizedApps = repository.getStudentApplications("other_student").first()
        assertTrue(unauthorizedApps.isEmpty())
    }

    @Test
    fun `withdrawApplication succeeds when application is in SUBMITTED state`() = runBlocking {
        val app = repository.submitApplication("internship_1").getOrThrow()

        val withdrawResult = repository.withdrawApplication(app.id)
        assertTrue(withdrawResult.isSuccess)

        val updatedApp = repository.getApplicationById(app.id).first()
        assertEquals(ApplicationStatus.WITHDRAWN, updatedApp?.status)
        assertFalse(updatedApp!!.isWithdrawable)

        val history = repository.getApplicationHistory(app.id).first()
        assertEquals(2, history.size)
        assertEquals(ApplicationStatus.SUBMITTED, history[0].status)
        assertEquals(ApplicationStatus.WITHDRAWN, history[1].status)
    }

    @Test
    fun `withdrawApplication fails when performed by another student UID`() = runBlocking {
        val app = repository.submitApplication("internship_1").getOrThrow()

        currentAuthUid = "attacker_student"
        val withdrawResult = repository.withdrawApplication(app.id)

        assertTrue(withdrawResult.isFailure)
        assertTrue(withdrawResult.exceptionOrNull() is ApplicationException.UnauthorizedAccess)
    }

    @Test
    fun `withdrawApplication fails when status is ACCEPTED`() = runBlocking {
        val app = repository.submitApplication("internship_1").getOrThrow()

        // Simulate provider accepting application
        fakeClient.updateStatusDirectly(app.id, ApplicationStatus.ACCEPTED)

        val withdrawResult = repository.withdrawApplication(app.id)
        assertTrue(withdrawResult.isFailure)
        assertTrue(withdrawResult.exceptionOrNull() is ApplicationException.NotEligibleForWithdrawal)
    }

    // Fake Client Implementation for Testing
    private class FakeFirebaseApplicationClient : FirebaseApplicationClient {
        override val isConfigured: Boolean = true
        private val applications = mutableMapOf<String, Map<String, Any?>>()
        private val histories = mutableMapOf<String, MutableList<Map<String, Any?>>>()

        override suspend fun submitApplicationTransaction(
            applicationId: String,
            applicationData: Map<String, Any?>,
            historyData: Map<String, Any?>
        ) {
            if (applications.containsKey(applicationId)) {
                throw ApplicationException.AlreadyApplied
            }
            applications[applicationId] = applicationData
            histories.getOrPut(applicationId) { mutableListOf() }.add(historyData)
        }

        override suspend fun hasStudentApplied(studentId: String, internshipId: String): Boolean {
            val appId = "${internshipId}_${studentId}"
            return applications.containsKey(appId)
        }

        override suspend fun getStudentApplications(studentId: String): List<Map<String, Any?>> {
            return applications.values.filter { it["studentId"] == studentId }
        }

        override suspend fun getApplicationById(applicationId: String): Map<String, Any?>? {
            return applications[applicationId]
        }

        override suspend fun getApplicationHistory(applicationId: String): List<Map<String, Any?>> {
            return histories[applicationId] ?: emptyList()
        }

        override suspend fun withdrawApplicationTransaction(
            applicationId: String,
            studentId: String,
            historyData: Map<String, Any?>
        ) {
            val app = applications[applicationId] ?: throw ApplicationException.ApplicationNotFound
            if (app["studentId"] != studentId) throw ApplicationException.UnauthorizedAccess
            val currentStatus = app["status"] as? String
            if (currentStatus in listOf("ACCEPTED", "REJECTED")) {
                throw ApplicationException.NotEligibleForWithdrawal
            }

            val updated = app.toMutableMap()
            updated["status"] = "WITHDRAWN"
            applications[applicationId] = updated
            histories.getOrPut(applicationId) { mutableListOf() }.add(historyData)
        }

        fun updateStatusDirectly(applicationId: String, newStatus: ApplicationStatus) {
            val app = applications[applicationId]?.toMutableMap() ?: return
            app["status"] = newStatus.name
            applications[applicationId] = app
        }
    }

    private class FakeInternshipRepository : InternshipRepository {
        private val internships = mutableListOf<Internship>()
        fun addInternship(internship: Internship) { internships.add(internship) }
        override val approvedInternships = flowOf(internships.filter { it.status == InternshipStatus.APPROVED })
        override suspend fun getApprovedInternships(): Result<List<Internship>> = Result.success(internships.filter { it.status == InternshipStatus.APPROVED })
        override suspend fun getInternshipById(id: String): Result<Internship?> = Result.success(internships.find { it.id == id })
        override suspend fun searchInternships(query: String, domain: String, district: String, workMode: String): Result<List<Internship>> = Result.success(internships)
    }

    private class FakeStudentProfileRepository : StudentProfileRepository {
        private var profile: StudentProfile? = null
        fun setProfile(studentProfile: StudentProfile) { profile = studentProfile }
        override val currentStudentProfile = flowOf(profile)
        override suspend fun getStudentProfile(studentId: String): Result<StudentProfile?> = Result.success(profile)
        override suspend fun createStudentProfile(profile: StudentProfile): Result<StudentProfile> = Result.success(profile)
        override suspend fun updateStudentProfile(profile: StudentProfile): Result<StudentProfile> = Result.success(profile)
        override suspend fun updateSkillProficiencies(studentId: String, skillProficiencies: List<SkillProficiency>): Result<List<SkillProficiency>> = Result.success(skillProficiencies)
    }
}
