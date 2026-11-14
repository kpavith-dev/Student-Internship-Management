package com.internmatch.app.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.internmatch.app.domain.repository.ApplicationRepository
import com.internmatch.app.domain.repository.AuthRepository
import com.internmatch.app.ui.application.ApplicationViewModel

/**
 * Factory for instantiating [ApplicationViewModel] using dependencies from [AppContainer].
 */
class ApplicationViewModelFactory(
    private val applicationRepository: ApplicationRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ApplicationViewModel::class.java)) {
            return ApplicationViewModel(
                applicationRepository = applicationRepository,
                authRepository = authRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
