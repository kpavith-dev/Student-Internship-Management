package com.internmatch.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentProfileTest {

    @Test
    fun studentProfile_instantiation_hasCorrectDefaultValues() {
        val profile = StudentProfile(
            studentId = "student_uid_123"
        )

        assertEquals("student_uid_123", profile.studentId)
        assertEquals("", profile.registrationNumber)
        assertEquals("", profile.university)
        assertEquals("", profile.degreeProgram)
        assertEquals(1, profile.academicYear)
        assertEquals(1, profile.semester)
        assertNull(profile.gpa)
        assertEquals("", profile.bio)
        assertNull(profile.contactDetails)
        assertTrue(profile.projects.isEmpty())
        assertTrue(profile.certificates.isEmpty())
        assertTrue(profile.skillProficiencies.isEmpty())
    }

    @Test
    fun studentProfile_instantiation_withFullFields() {
        val contact = ContactInfo(
            phone = "+94771234567",
            linkedinUrl = "https://linkedin.com/in/test",
            githubUrl = "https://github.com/test",
            portfolioUrl = "https://test.dev"
        )

        val project = Project(
            title = "InternMatch",
            description = "Mobile Internship Matching App",
            projectUrl = "https://github.com/test/internmatch"
        )

        val cert = Certificate(
            title = "Android Associate Developer",
            issuer = "Google",
            issueDate = "2024-01-15"
        )

        val skillProf = SkillProficiency(
            skillId = "kotlin",
            level = ProficiencyLevel.ADVANCED
        )

        val profile = StudentProfile(
            studentId = "student_uid_456",
            registrationNumber = "REG/2021/001",
            university = "University of Jaffna",
            degreeProgram = "Computer Engineering",
            academicYear = 3,
            semester = 2,
            gpa = 3.85,
            bio = "Passionate mobile developer",
            contactDetails = contact,
            projects = listOf(project),
            certificates = listOf(cert),
            skillProficiencies = listOf(skillProf)
        )

        assertEquals("student_uid_456", profile.studentId)
        assertEquals("REG/2021/001", profile.registrationNumber)
        assertEquals("University of Jaffna", profile.university)
        assertEquals("Computer Engineering", profile.degreeProgram)
        assertEquals(3, profile.academicYear)
        assertEquals(2, profile.semester)
        assertEquals(3.85, profile.gpa ?: 0.0, 0.001)
        assertEquals("Passionate mobile developer", profile.bio)
        assertNotNull(profile.contactDetails)
        assertEquals("+94771234567", profile.contactDetails?.phone)
        assertEquals(1, profile.projects.size)
        assertEquals(1, profile.certificates.size)
        assertEquals(1, profile.skillProficiencies.size)
        assertEquals("kotlin", profile.skillProficiencies.first().skillId)
        assertEquals(ProficiencyLevel.ADVANCED, profile.skillProficiencies.first().level)
    }

    @Test
    fun studentProfile_copyMethod_createsNewImmutableInstance() {
        val original = StudentProfile(
            studentId = "student_uid_789",
            university = "Original Uni",
            degreeProgram = "CS"
        )

        val updated = original.copy(
            university = "Updated Uni",
            gpa = 3.90
        )

        assertEquals("Original Uni", original.university)
        assertNull(original.gpa)
        assertEquals("Updated Uni", updated.university)
        assertEquals(3.90, updated.gpa ?: 0.0, 0.001)
    }
}
