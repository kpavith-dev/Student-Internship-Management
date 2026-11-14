package com.internmatch.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationTest {

    @Test
    fun `isWithdrawable returns true for active states`() {
        val submittedApp = Application(status = ApplicationStatus.SUBMITTED)
        val underReviewApp = Application(status = ApplicationStatus.UNDER_REVIEW)
        val shortlistedApp = Application(status = ApplicationStatus.SHORTLISTED)

        assertTrue(submittedApp.isWithdrawable)
        assertTrue(underReviewApp.isWithdrawable)
        assertTrue(shortlistedApp.isWithdrawable)
    }

    @Test
    fun `isWithdrawable returns false for terminal states`() {
        val acceptedApp = Application(status = ApplicationStatus.ACCEPTED)
        val rejectedApp = Application(status = ApplicationStatus.REJECTED)
        val withdrawnApp = Application(status = ApplicationStatus.WITHDRAWN)

        assertFalse(acceptedApp.isWithdrawable)
        assertFalse(rejectedApp.isWithdrawable)
        assertFalse(withdrawnApp.isWithdrawable)
    }

    @Test
    fun `StudentProfileSnapshot captures student profile attributes accurately`() {
        val profile = StudentProfile(
            studentId = "student_123",
            registrationNumber = "REG/2023/001",
            university = "University of Jaffna",
            degreeProgram = "Computer Science",
            academicYear = 3,
            semester = 2,
            gpa = 3.8,
            bio = "Enthusiastic mobile developer",
            contactDetails = ContactInfo(phone = "0771234567"),
            skillProficiencies = listOf(
                SkillProficiency(skillId = "kotlin", level = ProficiencyLevel.ADVANCED)
            )
        )

        val snapshot = StudentProfileSnapshot.fromStudentProfile(profile)

        assertEquals("student_123", snapshot.studentId)
        assertEquals("REG/2023/001", snapshot.registrationNumber)
        assertEquals("University of Jaffna", snapshot.university)
        assertEquals("Computer Science", snapshot.degreeProgram)
        assertEquals(3, snapshot.academicYear)
        assertEquals(2, snapshot.semester)
        assertEquals(3.8, snapshot.gpa)
        assertEquals(1, snapshot.skillProficiencies.size)
        assertEquals("kotlin", snapshot.skillProficiencies[0].skillId)
    }

    @Test
    fun `ApplicationStatusHistory captures transition metadata correctly`() {
        val history = ApplicationStatusHistory(
            id = "hist_1",
            applicationId = "app_1",
            status = ApplicationStatus.SUBMITTED,
            timestamp = 1700000000000L,
            actorUid = "student_123",
            note = "Submitted application"
        )

        assertEquals("hist_1", history.id)
        assertEquals("app_1", history.applicationId)
        assertEquals(ApplicationStatus.SUBMITTED, history.status)
        assertEquals(1700000000000L, history.timestamp)
        assertEquals("student_123", history.actorUid)
        assertEquals("Submitted application", history.note)
    }
}
