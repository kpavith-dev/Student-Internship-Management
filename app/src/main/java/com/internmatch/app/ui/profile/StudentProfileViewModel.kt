package com.internmatch.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.internmatch.app.domain.model.ContactInfo
import com.internmatch.app.domain.model.ProficiencyLevel
import com.internmatch.app.domain.model.SkillProficiency
import com.internmatch.app.domain.model.StudentProfile
import com.internmatch.app.domain.repository.AuthRepository
import com.internmatch.app.domain.repository.SkillRepository
import com.internmatch.app.domain.repository.StudentProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing Student Profile loading, creation, editing, validation,
 * and skill proficiency management.
 *
 * Features & Lifecycle Boundaries:
 * - Depends strictly on domain repository interfaces following MVVM architecture.
 * - Protects against async race conditions during user switching / logout.
 * - Cancels in-flight load requests on logout or user change.
 * - Resets profile state immediately on sign out.
 */
class StudentProfileViewModel(
    private val profileRepository: StudentProfileRepository,
    private val authRepository: AuthRepository,
    private val skillRepository: SkillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentProfileUiState())
    val uiState: StateFlow<StudentProfileUiState> = _uiState.asStateFlow()

    internal var activeProfileLoadJob: Job? = null
        private set

    init {
        observeCurrentUserAndLoadProfile()
        loadMasterSkills()
    }

    private fun observeCurrentUserAndLoadProfile() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                val currentProfileUid = _uiState.value.profile?.studentId
                if (user == null) {
                    activeProfileLoadJob?.cancel()
                    _uiState.update {
                        it.copy(
                            currentUser = null,
                            profile = null,
                            isEditing = false,
                            isCreatingProfile = false,
                            isLoading = false,
                            errorMessage = null,
                            successMessage = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(currentUser = user) }
                    if (user.id != currentProfileUid) {
                        _uiState.update { it.copy(profile = null) }
                        loadProfile(user.id)
                    }
                }
            }
        }
    }

    private fun loadMasterSkills() {
        viewModelScope.launch {
            val skills = skillRepository.getMasterSkills()
            _uiState.update { it.copy(availableSkills = skills) }
        }
    }

    fun loadProfile(studentId: String) {
        if (studentId.isBlank()) return

        activeProfileLoadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        activeProfileLoadJob = viewModelScope.launch {
            val result = profileRepository.getStudentProfile(studentId)
            applyProfileLoadResult(studentId, result)
        }
    }

    internal fun applyProfileLoadResult(studentId: String, result: Result<StudentProfile?>) {
        // Async race check: verify active user is still the target studentId
        if (_uiState.value.currentUser?.id == studentId) {
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            isCreatingProfile = (profile == null)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load profile."
                        )
                    }
                }
            )
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true, validationErrors = emptyMap(), errorMessage = null) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false, validationErrors = emptyMap(), errorMessage = null) }
    }

    fun saveProfile(
        registrationNumber: String,
        university: String,
        degreeProgram: String,
        academicYear: Int,
        semester: Int,
        gpa: Double?,
        bio: String,
        phone: String = "",
        linkedinUrl: String = "",
        githubUrl: String = "",
        portfolioUrl: String = ""
    ) {
        val user = _uiState.value.currentUser
        if (user == null) {
            _uiState.update { it.copy(errorMessage = "User is not authenticated.") }
            return
        }

        val errors = validateProfileFields(
            registrationNumber = registrationNumber,
            university = university,
            degreeProgram = degreeProgram,
            academicYear = academicYear,
            semester = semester,
            gpa = gpa
        )

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors, errorMessage = "Please correct validation errors.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, validationErrors = emptyMap()) }

        val currentProfile = _uiState.value.profile
        val isCreating = _uiState.value.isCreatingProfile
        val updatedProfile = StudentProfile(
            studentId = user.id,
            registrationNumber = registrationNumber.trim(),
            university = university.trim(),
            degreeProgram = degreeProgram.trim(),
            academicYear = academicYear,
            semester = semester,
            gpa = gpa,
            bio = bio.trim(),
            contactDetails = ContactInfo(
                phone = phone.trim(),
                linkedinUrl = linkedinUrl.trim(),
                githubUrl = githubUrl.trim(),
                portfolioUrl = portfolioUrl.trim()
            ),
            projects = currentProfile?.projects ?: emptyList(),
            certificates = currentProfile?.certificates ?: emptyList(),
            skillProficiencies = currentProfile?.skillProficiencies ?: emptyList()
        )

        viewModelScope.launch {
            val result = if (isCreating) {
                profileRepository.createStudentProfile(updatedProfile)
            } else {
                profileRepository.updateStudentProfile(updatedProfile)
            }

            if (_uiState.value.currentUser?.id == user.id) {
                result.fold(
                    onSuccess = { saved ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                profile = saved,
                                isEditing = false,
                                isCreatingProfile = false,
                                successMessage = "Profile saved successfully."
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to save profile."
                            )
                        }
                    }
                )
            }
        }
    }

    fun addSkillProficiency(skillId: String, level: ProficiencyLevel) {
        val user = _uiState.value.currentUser ?: return
        val currentProfile = _uiState.value.profile ?: StudentProfile(studentId = user.id)

        if (skillId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Skill ID cannot be empty.") }
            return
        }

        // Prevent duplicate selection of the same skill
        if (currentProfile.skillProficiencies.any { it.skillId == skillId }) {
            _uiState.update { it.copy(errorMessage = "Skill is already added to your profile.") }
            return
        }

        val updatedProficiencies = currentProfile.skillProficiencies + SkillProficiency(skillId, level)
        updateSkillsOnRepository(user.id, updatedProficiencies)
    }

    fun updateSkillProficiency(skillId: String, level: ProficiencyLevel) {
        val user = _uiState.value.currentUser ?: return
        val currentProfile = _uiState.value.profile ?: return

        val updatedProficiencies = currentProfile.skillProficiencies.map { sp ->
            if (sp.skillId == skillId) sp.copy(level = level) else sp
        }

        updateSkillsOnRepository(user.id, updatedProficiencies)
    }

    fun removeSkillProficiency(skillId: String) {
        val user = _uiState.value.currentUser ?: return
        val currentProfile = _uiState.value.profile ?: return

        val updatedProficiencies = currentProfile.skillProficiencies.filterNot { it.skillId == skillId }
        updateSkillsOnRepository(user.id, updatedProficiencies)
    }

    private fun updateSkillsOnRepository(studentId: String, proficiencies: List<SkillProficiency>) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = profileRepository.updateSkillProficiencies(studentId, proficiencies)
            if (_uiState.value.currentUser?.id == studentId) {
                result.fold(
                    onSuccess = { updated ->
                        val currentProf = _uiState.value.profile ?: StudentProfile(studentId = studentId)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                profile = currentProf.copy(skillProficiencies = updated),
                                successMessage = "Skills updated successfully."
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to update skills."
                            )
                        }
                    }
                )
            }
        }
    }

    fun validateProfileFields(
        registrationNumber: String,
        university: String,
        degreeProgram: String,
        academicYear: Int,
        semester: Int,
        gpa: Double?
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (registrationNumber.isBlank()) {
            errors["registrationNumber"] = "Registration number is required."
        }
        if (university.isBlank()) {
            errors["university"] = "University name is required."
        }
        if (degreeProgram.isBlank()) {
            errors["degreeProgram"] = "Degree programme is required."
        }
        if (academicYear !in 1..6) {
            errors["academicYear"] = "Academic year must be between 1 and 6."
        }
        if (semester !in 1..12) {
            errors["semester"] = "Semester must be between 1 and 12."
        }
        if (gpa != null && (gpa < 0.0 || gpa > 4.0)) {
            errors["gpa"] = "GPA must be between 0.0 and 4.0."
        }

        return errors
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
