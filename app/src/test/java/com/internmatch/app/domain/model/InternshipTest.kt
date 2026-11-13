package com.internmatch.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InternshipTest {

    @Test
    fun `internship default values verify FR16 proposed schema extensions`() {
        val internship = Internship(
            id = "intern_001",
            title = "Android Developer Intern",
            companyName = "TechCorp"
        )

        assertEquals("intern_001", internship.id)
        assertEquals("Android Developer Intern", internship.title)
        assertEquals("TechCorp", internship.companyName)
        assertEquals("", internship.domain)
        assertEquals("", internship.district)
        assertEquals("", internship.workMode)
        assertEquals(InternshipStatus.APPROVED, internship.status)
        assertTrue(internship.requiredSkills.isEmpty())
    }

    @Test
    fun `internship explicitly assigned domain district and work mode`() {
        val internship = Internship(
            id = "intern_002",
            title = "Backend Engineer Intern",
            companyName = "DataSystems",
            domain = "Software Engineering",
            district = "Colombo",
            workMode = "HYBRID",
            employmentType = "FULL_TIME"
        )

        assertEquals("Software Engineering", internship.domain)
        assertEquals("Colombo", internship.district)
        assertEquals("HYBRID", internship.workMode)
        assertEquals("FULL_TIME", internship.employmentType)
    }

    @Test
    fun `bookmark creation initializes internshipId and savedAt timestamp`() {
        val bookmark = Bookmark(
            internshipId = "intern_100",
            savedAt = 1740000000000L
        )

        assertEquals("intern_100", bookmark.internshipId)
        assertEquals(1740000000000L, bookmark.savedAt)
    }
}
