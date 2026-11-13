package com.internmatch.app.data.repository

import com.internmatch.app.domain.model.Certificate
import com.internmatch.app.domain.model.ContactInfo
import com.internmatch.app.domain.model.ProficiencyLevel
import com.internmatch.app.domain.model.Project
import com.internmatch.app.domain.model.SkillProficiency
import com.internmatch.app.domain.model.StudentProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeFirebaseStudentProfileClient(
    var isConfiguredOverride: Boolean = true,
    var shouldFailWith: Throwable? = null
) : FirebaseStudentProfileClient {

    val store = mutableMapOf<String, Map<String, Any?>>()

    override val isConfigured: Boolean
        get() = isConfiguredOverride

    override suspend fun getProfile(studentId: String): Map<String, Any?>? {
        shouldFailWith?.let { throw it }
        val doc = store[studentId] ?: return null
        val storedOwnerId = doc["studentId"] as? String
        if (storedOwnerId.isNullOrBlank() || storedOwnerId != studentId) {
            throw SecurityException("Stored document owner UID '$storedOwnerId' is missing or does not match requested document path UID '$studentId'.")
        }
        return doc
    }

    override suspend fun createProfile(studentId: String, data: Map<String, Any?>) {
        shouldFailWith?.let { throw it }
        val dataOwnerId = data["studentId"] as? String
        if (dataOwnerId.isNullOrBlank() || dataOwnerId != studentId) {
            throw IllegalArgumentException("Owner UID mismatch or missing in create payload: data studentId '$dataOwnerId' != path studentId '$studentId'.")
        }
        if (store.containsKey(studentId)) {
            throw IllegalStateException("Profile already exists for student UID '$studentId'. Use updateProfile instead.")
        }
        store[studentId] = data
    }

    override suspend fun updateProfile(studentId: String, data: Map<String, Any?>) {
        shouldFailWith?.let { throw it }
        if (data.containsKey("studentId")) {
            val dataOwnerId = data["studentId"] as? String
            if (dataOwnerId.isNullOrBlank() || dataOwnerId != studentId) {
                throw IllegalArgumentException("Invalid or mismatched studentId in update payload: key is present but value '${data["studentId"]}' is not a non-blank String matching '$studentId'.")
            }
        }

        val existing = store[studentId]?.toMutableMap()
            ?: throw NoSuchElementException("Student profile does not exist for UID '$studentId'. Use createProfile instead.")
        val existingOwnerId = existing["studentId"] as? String
        if (existingOwnerId.isNullOrBlank() || existingOwnerId != studentId) {
            throw SecurityException("Owner UID mismatch in stored document: '$existingOwnerId' != '$studentId'.")
        }
        existing.putAll(data)
        store[studentId] = existing
    }

    override suspend fun updateSkillProficiencies(
        studentId: String,
        skillProficienciesData: List<Map<String, Any?>>
    ) {
        shouldFailWith?.let { throw it }
        val current = store[studentId]?.toMutableMap()
            ?: throw NoSuchElementException("Student profile does not exist for UID '$studentId'. Create a profile before updating skills.")
        val storedOwnerId = current["studentId"] as? String
        if (storedOwnerId.isNullOrBlank() || storedOwnerId != studentId) {
            throw SecurityException("Owner UID mismatch in stored document: '$storedOwnerId' != '$studentId'.")
        }
        current["skillProficiencies"] = skillProficienciesData
        store[studentId] = current
    }
}

class FirestoreStudentProfileRepositoryTest {

    private lateinit var fakeClient: FakeFirebaseStudentProfileClient
    private var authenticatedUid: String? = "student_1"
    private lateinit var repository: FirestoreStudentProfileRepository

    @Before
    fun setUp() {
        fakeClient = FakeFirebaseStudentProfileClient()
        authenticatedUid = "student_1"
        repository = FirestoreStudentProfileRepository(
            profileClient = fakeClient,
            authenticatedUserIdProvider = { authenticatedUid }
        )
    }

    @Test
    fun getStudentProfile_blankStudentId_returnsFailure() = runTest {
        val result = repository.getStudentProfile("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun getStudentProfile_unauthenticated_returnsSecurityError() = runTest {
        authenticatedUid = null

        val result = repository.getStudentProfile("student_1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun getStudentProfile_mismatchedUid_returnsSecurityError() = runTest {
        authenticatedUid = "student_1"

        val result = repository.getStudentProfile("student_999")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun getStudentProfile_legacyDocumentMissingOwnerId_returnsSecurityError() = runTest {
        authenticatedUid = "student_1"
        fakeClient.store["student_1"] = mapOf("university" to "Test Uni", "degreeProgram" to "CS")

        val result = repository.getStudentProfile("student_1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun getStudentProfile_storedOwnerNull_returnsSecurityError() = runTest {
        authenticatedUid = "student_1"
        fakeClient.store["student_1"] = mapOf("studentId" to null, "university" to "Test Uni", "degreeProgram" to "CS")

        val result = repository.getStudentProfile("student_1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun getStudentProfile_storedOwnerBlank_returnsSecurityError() = runTest {
        authenticatedUid = "student_1"
        fakeClient.store["student_1"] = mapOf("studentId" to "   ", "university" to "Test Uni", "degreeProgram" to "CS")

        val result = repository.getStudentProfile("student_1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun getStudentProfile_storedOwnerInteger_returnsSecurityError() = runTest {
        authenticatedUid = "student_1"
        fakeClient.store["student_1"] = mapOf("studentId" to 12345, "university" to "Test Uni", "degreeProgram" to "CS")

        val result = repository.getStudentProfile("student_1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun getStudentProfile_storedOwnerMismatched_returnsSecurityError() = runTest {
        authenticatedUid = "student_1"
        fakeClient.store["student_1"] = mapOf("studentId" to "student_999", "university" to "Test Uni", "degreeProgram" to "CS")

        val result = repository.getStudentProfile("student_1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun getStudentProfile_unconfiguredClient_returnsIllegalStateException() = runTest {
        val unconfiguredClient = FakeFirebaseStudentProfileClient(isConfiguredOverride = false)
        val unconfiguredRepo = FirestoreStudentProfileRepository(
            profileClient = unconfiguredClient,
            authenticatedUserIdProvider = { "student_1" }
        )
        val result = unconfiguredRepo.getStudentProfile("student_1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun getStudentProfile_authenticatedOwner_returnsSuccessWithProfile() = runTest {
        val profileData = mapOf<String, Any?>(
            "studentId" to "student_1",
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "Engineering",
            "academicYear" to 2L,
            "semester" to 1L,
            "gpa" to 3.75,
            "bio" to "Hello World",
            "skillProficiencies" to listOf(
                mapOf("skillId" to "kotlin", "level" to "ADVANCED")
            )
        )
        fakeClient.store["student_1"] = profileData

        val result = repository.getStudentProfile("student_1")
        assertTrue(result.isSuccess)

        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals("student_1", profile?.studentId)
        assertEquals("REG/001", profile?.registrationNumber)
        assertEquals("Test Uni", profile?.university)
        assertEquals("Engineering", profile?.degreeProgram)
        assertEquals(2, profile?.academicYear)
        assertEquals(1, profile?.semester)
        assertEquals(3.75, profile?.gpa ?: 0.0, 0.001)
        assertEquals(1, profile?.skillProficiencies?.size)
        assertEquals("kotlin", profile?.skillProficiencies?.first()?.skillId)
        assertEquals(ProficiencyLevel.ADVANCED, profile?.skillProficiencies?.first()?.level)

        val observed = repository.currentStudentProfile.first()
        assertEquals(profile, observed)
    }

    @Test
    fun createStudentProfile_existingProfile_returnsFailure() = runTest {
        authenticatedUid = "student_1"
        fakeClient.store["student_1"] = mapOf("studentId" to "student_1", "registrationNumber" to "REG/1", "university" to "Uni", "degreeProgram" to "CS")

        val newProfile = StudentProfile(
            studentId = "student_1",
            registrationNumber = "REG/1",
            university = "New Uni",
            degreeProgram = "CS"
        )

        val result = repository.createStudentProfile(newProfile)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun updateStudentProfile_missingProfile_returnsNoSuchElementException() = runTest {
        authenticatedUid = "student_1"

        val profile = StudentProfile(
            studentId = "student_1",
            registrationNumber = "REG/1",
            university = "New Uni",
            degreeProgram = "CS"
        )

        val result = repository.updateStudentProfile(profile)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun updateStudentProfile_existingProfile_updatesSuccessfully() = runTest {
        authenticatedUid = "student_1"
        fakeClient.store["student_1"] = mapOf("studentId" to "student_1", "registrationNumber" to "REG/1", "university" to "Old Uni", "degreeProgram" to "CS")

        val updatedProfile = StudentProfile(
            studentId = "student_1",
            registrationNumber = "REG/1",
            university = "Updated Uni",
            degreeProgram = "CS",
            academicYear = 4,
            semester = 2
        )

        val result = repository.updateStudentProfile(updatedProfile)
        assertTrue(result.isSuccess)
        assertEquals("Updated Uni", fakeClient.store["student_1"]?.get("university"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun clientUpdateProfile_blankStudentIdInPayload_throwsIllegalArgumentException() = runTest {
        fakeClient.store["student_1"] = mapOf("studentId" to "student_1", "university" to "Uni")
        fakeClient.updateProfile("student_1", mapOf("studentId" to "", "university" to "New Uni"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun clientUpdateProfile_nullStudentIdInPayload_throwsIllegalArgumentException() = runTest {
        fakeClient.store["student_1"] = mapOf("studentId" to "student_1", "university" to "Uni")
        fakeClient.updateProfile("student_1", mapOf("studentId" to null, "university" to "New Uni"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun clientUpdateProfile_numericStudentIdInPayload_throwsIllegalArgumentException() = runTest {
        fakeClient.store["student_1"] = mapOf("studentId" to "student_1", "university" to "Uni")
        fakeClient.updateProfile("student_1", mapOf("studentId" to 12345, "university" to "New Uni"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun clientUpdateProfile_mismatchedStudentIdInPayload_throwsIllegalArgumentException() = runTest {
        fakeClient.store["student_1"] = mapOf("studentId" to "student_1", "university" to "Uni")
        fakeClient.updateProfile("student_1", mapOf("studentId" to "student_999", "university" to "New Uni"))
    }

    @Test
    fun clientUpdateProfile_missingStudentIdInPayload_preservesExistingOwner() = runTest {
        fakeClient.store["student_1"] = mapOf("studentId" to "student_1", "university" to "Old Uni")
        fakeClient.updateProfile("student_1", mapOf("university" to "Updated Uni"))

        val stored = fakeClient.store["student_1"]
        assertEquals("student_1", stored?.get("studentId"))
        assertEquals("Updated Uni", stored?.get("university"))
    }

    @Test
    fun updateSkillProficiencies_missingProfileDoc_returnsNoSuchElementException() = runTest {
        authenticatedUid = "student_1"
        val proficiencies = listOf(
            SkillProficiency(skillId = "android", level = ProficiencyLevel.EXPERT)
        )

        val result = repository.updateSkillProficiencies("student_1", proficiencies)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun updateSkillProficiencies_existingProfile_updatesSkills() = runTest {
        authenticatedUid = "student_1"
        fakeClient.store["student_1"] = mapOf("studentId" to "student_1", "registrationNumber" to "REG/1", "university" to "Uni", "degreeProgram" to "CS")

        val proficiencies = listOf(
            SkillProficiency(skillId = "android", level = ProficiencyLevel.EXPERT),
            SkillProficiency(skillId = "jetpack-compose", level = ProficiencyLevel.ADVANCED)
        )

        val result = repository.updateSkillProficiencies("student_1", proficiencies)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)

        val currentProfile = repository.currentStudentProfile.first()
        assertEquals("student_1", currentProfile?.studentId)
        assertEquals(2, currentProfile?.skillProficiencies?.size)
    }

    // MAPPER TESTS – REQUIRED PROFILE FIELDS
    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_registrationNumberMissing_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_registrationNumberBlank_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "   ",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_registrationNumberWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to 12345,
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_universityMissing_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_universityBlank_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_universityWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to true,
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_degreeProgramMissing_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "academicYear" to 2L,
            "semester" to 1L
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_degreeProgramBlank_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "   ",
            "academicYear" to 2L,
            "semester" to 1L
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_degreeProgramWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to 9999,
            "academicYear" to 2L,
            "semester" to 1L
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    // MAPPER TESTS – ACADEMIC YEAR, SEMESTER & GPA
    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_decimalAcademicYear_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 3.5,
            "semester" to 1L
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_decimalSemester_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 3L,
            "semester" to 2.5
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_corruptedGpaOutOfBounds_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "gpa" to 5.5
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    // MAPPER TESTS – CONTACT DETAILS
    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_contactDetailsNonMap_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "contactDetails" to "INVALID_NON_MAP_STRING"
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_phoneWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "contactDetails" to mapOf("phone" to 771234567)
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_linkedinUrlWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "contactDetails" to mapOf("linkedinUrl" to 12345)
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_githubUrlWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "contactDetails" to mapOf("githubUrl" to true)
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_portfolioUrlWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "contactDetails" to mapOf("portfolioUrl" to listOf("https://portfolio.dev"))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test
    fun mapDataToStudentProfile_contactDetailsOptionalFieldsMissing_defaultsToEmptyStrings() {
        val data = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "contactDetails" to emptyMap<String, Any?>()
        )
        val profile = repository.mapDataToStudentProfile("student_1", data)
        assertNotNull(profile.contactDetails)
        assertEquals("", profile.contactDetails?.phone)
        assertEquals("", profile.contactDetails?.linkedinUrl)
        assertEquals("", profile.contactDetails?.githubUrl)
        assertEquals("", profile.contactDetails?.portfolioUrl)
    }

    // MAPPER TESTS – PROJECTS
    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_projectsNonList_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "projects" to "INVALID_NON_LIST_STRING"
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_projectMissingTitle_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "projects" to listOf(mapOf("description" to "No Title"))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_projectsNonMapListEntry_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "projects" to listOf("NOT_A_MAP_ENTRY")
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_projectDescriptionWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "projects" to listOf(mapOf("title" to "Title", "description" to 12345))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_projectUrlWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "projects" to listOf(mapOf("title" to "Title", "projectUrl" to false))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    // MAPPER TESTS – CERTIFICATES
    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_certificatesNonList_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "certificates" to 12345
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_certificatesNonMapListEntry_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "certificates" to listOf("NOT_A_MAP_ENTRY")
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_certificateMissingTitle_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "certificates" to listOf(mapOf("issuer" to "Google"))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_certificateIssuerWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "certificates" to listOf(mapOf("title" to "Title", "issuer" to 9999))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_certificateIssueDateWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "certificates" to listOf(mapOf("title" to "Title", "issueDate" to listOf("2024-01-01")))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    // MAPPER TESTS – SKILL PROFICIENCY
    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_skillProficienciesNonList_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "skillProficiencies" to "INVALID_NON_LIST_STRING"
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_skillProficiencyEntryNonMap_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "skillProficiencies" to listOf("NOT_A_MAP_ENTRY")
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_skillIdMissing_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "skillProficiencies" to listOf(mapOf("level" to "ADVANCED"))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_skillIdBlank_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "skillProficiencies" to listOf(mapOf("skillId" to "  ", "level" to "ADVANCED"))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_skillIdWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "skillProficiencies" to listOf(mapOf("skillId" to 12345, "level" to "ADVANCED"))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_skillLevelMissing_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "skillProficiencies" to listOf(mapOf("skillId" to "kotlin"))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_skillLevelWrongType_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "skillProficiencies" to listOf(mapOf("skillId" to "kotlin", "level" to 555))
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapDataToStudentProfile_corruptedProficiencyLevelString_throwsIllegalArgumentException() {
        val corruptedData = mapOf<String, Any?>(
            "registrationNumber" to "REG/001",
            "university" to "Test Uni",
            "degreeProgram" to "CS",
            "academicYear" to 2L,
            "semester" to 1L,
            "skillProficiencies" to listOf(
                mapOf("skillId" to "kotlin", "level" to "CORRUPTED_LEVEL_STRING")
            )
        )
        repository.mapDataToStudentProfile("student_1", corruptedData)
    }

    // MAPPER TESTS – ROUND TRIP
    @Test
    fun mapDataToStudentProfile_roundTrip_handlesAllFieldsAndDefaults() {
        val contact = ContactInfo(
            phone = "+94770000000",
            linkedinUrl = "https://linkedin.com/in/test",
            githubUrl = "https://github.com/test",
            portfolioUrl = "https://portfolio.dev"
        )
        val project = Project("App", "Desc", "https://url")
        val cert = Certificate("Cert", "Issuer", "2024")
        val skill = SkillProficiency("kotlin", ProficiencyLevel.EXPERT)

        val profile = StudentProfile(
            studentId = "student_1",
            registrationNumber = "REG/123",
            university = "Jaffna",
            degreeProgram = "CE",
            academicYear = 4,
            semester = 1,
            gpa = 3.9,
            bio = "Bio",
            contactDetails = contact,
            projects = listOf(project),
            certificates = listOf(cert),
            skillProficiencies = listOf(skill)
        )

        val data = repository.mapStudentProfileToData(profile)
        val restored = repository.mapDataToStudentProfile("student_1", data)

        assertEquals(profile, restored)
    }

    @Test(expected = CancellationException::class)
    fun repository_cancellationException_isRethrown() = runTest {
        authenticatedUid = "student_1"
        fakeClient.shouldFailWith = CancellationException("Job cancelled")
        repository.getStudentProfile("student_1")
    }
}
