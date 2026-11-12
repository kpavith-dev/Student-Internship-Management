package com.internmatch.app.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.internmatch.app.domain.repository.AuthRepository
import com.internmatch.app.domain.repository.SkillRepository
import com.internmatch.app.domain.repository.StudentProfileRepository
import com.internmatch.app.ui.profile.StudentProfileViewModel

/**
 * [ViewModelProvider.Factory] for instantiating [StudentProfileViewModel]
 * with injected domain repository dependencies.
 */
class ProfileViewModelFactory(
    private val profileRepository: StudentProfileRepository,
    private val authRepository: AuthRepository,
    private val skillRepository: SkillRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentProfileViewModel::class.java)) {
            return StudentProfileViewModel(
                profileRepository = profileRepository,
                authRepository = authRepository,
                skillRepository = skillRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
