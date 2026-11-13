package com.internmatch.app.ui.profile

import com.internmatch.app.domain.model.Skill
import com.internmatch.app.domain.model.StudentProfile
import com.internmatch.app.domain.model.User

/**
 * Immutable UI State data class for the Student Profile & Skills feature.
 */
data class StudentProfileUiState(
    val isLoading: Boolean = false,
    val profile: StudentProfile? = null,
    val currentUser: User? = null,
    val availableSkills: List<Skill> = emptyList(),
    val isEditing: Boolean = false,
    val isCreatingProfile: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)
