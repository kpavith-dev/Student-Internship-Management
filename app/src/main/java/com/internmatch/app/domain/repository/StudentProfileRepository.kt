package com.internmatch.app.domain.repository

import com.internmatch.app.domain.model.SkillProficiency
import com.internmatch.app.domain.model.StudentProfile
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing student profile records,
 * academic details, personal information, and skill proficiencies.
 */
interface StudentProfileRepository {
    /**
     * Flow observing the currently active student's profile.
     */
    val currentStudentProfile: Flow<StudentProfile?>

    /**
     * Retrieves a student profile by authenticated student UID.
     */
    suspend fun getStudentProfile(studentId: String): Result<StudentProfile?>

    /**
     * Creates a new student profile record.
     */
    suspend fun createStudentProfile(profile: StudentProfile): Result<StudentProfile>

    /**
     * Updates an existing student profile record.
     */
    suspend fun updateStudentProfile(profile: StudentProfile): Result<StudentProfile>

    /**
     * Updates the skill proficiencies for the specified student.
     */
    suspend fun updateSkillProficiencies(
        studentId: String,
        skillProficiencies: List<SkillProficiency>
    ): Result<List<SkillProficiency>>
}
