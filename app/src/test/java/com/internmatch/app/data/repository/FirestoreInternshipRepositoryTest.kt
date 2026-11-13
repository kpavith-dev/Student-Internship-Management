package com.internmatch.app.data.repository

import com.internmatch.app.domain.model.InternshipStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeFirebaseInternshipClient : FirebaseInternshipClient {
    override var isConfigured: Boolean = true
    var internshipsData = mutableListOf<Map<String, Any?>>()
    var shouldFail: Boolean = false

    override suspend fun getApprovedInternships(limit: Long): List<Map<String, Any?>> {
        if (shouldFail) throw RuntimeException("Firestore connection error")
        return internshipsData.filter { map ->
            val status = map["status"] as? String ?: "APPROVED"
            status == "APPROVED"
        }
    }
}

class FirestoreInternshipRepositoryTest {

    private lateinit var fakeClient: FakeFirebaseInternshipClient
    private lateinit var repository: FirestoreInternshipRepository

    @Before
    fun setUp() {
        fakeClient = FakeFirebaseInternshipClient()
        repository = FirestoreInternshipRepository(fakeClient)

        fakeClient.internshipsData = mutableListOf(
            mapOf(
                "id" to "intern_01",
                "title" to "Android Mobile Engineer",
                "companyName" to "TechCorp Solutions",
                "description" to "Develop native Android apps with Kotlin and Jetpack Compose",
                "location" to "Colombo 03",
                "domain" to "Software Engineering",
                "district" to "Colombo",
                "workMode" to "REMOTE",
                "status" to "APPROVED",
                "requiredSkills" to listOf(
                    mapOf("skillId" to "kotlin", "weight" to 1.0),
                    mapOf("skillId" to "compose", "weight" to 1.0)
                )
            ),
            mapOf(
                "id" to "intern_02",
                "title" to "Data Analyst Intern",
                "companyName" to "Analytics AI",
                "description" to "Perform SQL data queries and Python visualization",
                "location" to "Kandy Town",
                "domain" to "Data Science",
                "district" to "Kandy",
                "workMode" to "HYBRID",
                "status" to "APPROVED",
                "requiredSkills" to listOf(
                    mapOf("skillId" to "python", "weight" to 1.0),
                    mapOf("skillId" to "sql", "weight" to 1.0)
                )
            ),
            mapOf(
                "id" to "intern_03",
                "title" to "Unapproved Draft Intern",
                "companyName" to "Secret Labs",
                "description" to "Internal testing post",
                "location" to "Gampaha",
                "domain" to "Software Engineering",
                "district" to "Gampaha",
                "workMode" to "ON_SITE",
                "status" to "DRAFT"
            )
        )
    }

    @Test
    fun `getApprovedInternships returns only APPROVED status internships`() = runTest {
        val result = repository.getApprovedInternships()
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(2, list.size)
        assertTrue(list.all { it.status == InternshipStatus.APPROVED })
    }

    @Test
    fun `searchInternships filters by title keyword case-insensitive`() = runTest {
        val result = repository.searchInternships(query = "android")
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        assertEquals("Android Mobile Engineer", list[0].title)
    }

    @Test
    fun `searchInternships filters by company keyword case-insensitive`() = runTest {
        val result = repository.searchInternships(query = "analytics")
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        assertEquals("Data Analyst Intern", list[0].title)
    }

    @Test
    fun `searchInternships filters by domain district and work mode`() = runTest {
        val result = repository.searchInternships(
            domain = "Software Engineering",
            district = "Colombo",
            workMode = "REMOTE"
        )
        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        assertEquals("intern_01", list[0].id)
    }

    @Test
    fun `searchInternships returns empty list when no matches exist`() = runTest {
        val result = repository.searchInternships(query = "NonexistentQuery")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `searchInternships handles client failure gracefully`() = runTest {
        fakeClient.shouldFail = true
        val result = repository.getApprovedInternships()
        assertTrue(result.isFailure)
    }
}
