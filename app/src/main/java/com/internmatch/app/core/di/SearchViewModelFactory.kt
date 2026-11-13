package com.internmatch.app.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.internmatch.app.domain.repository.AuthRepository
import com.internmatch.app.domain.repository.BookmarkRepository
import com.internmatch.app.domain.repository.InternshipRepository
import com.internmatch.app.domain.repository.StudentProfileRepository
import com.internmatch.app.domain.service.MatchScoreProvider
import com.internmatch.app.ui.bookmark.BookmarkViewModel
import com.internmatch.app.ui.search.InternshipSearchViewModel

/**
 * Factory for instantiating [InternshipSearchViewModel] and [BookmarkViewModel]
 * using dependencies from [AppContainer].
 */
class SearchViewModelFactory(
    private val internshipRepository: InternshipRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val authRepository: AuthRepository,
    private val matchScoreProvider: MatchScoreProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(InternshipSearchViewModel::class.java) -> {
                InternshipSearchViewModel(
                    internshipRepository = internshipRepository,
                    bookmarkRepository = bookmarkRepository,
                    studentProfileRepository = studentProfileRepository,
                    authRepository = authRepository,
                    matchScoreProvider = matchScoreProvider
                ) as T
            }
            modelClass.isAssignableFrom(BookmarkViewModel::class.java) -> {
                BookmarkViewModel(
                    bookmarkRepository = bookmarkRepository,
                    internshipRepository = internshipRepository,
                    studentProfileRepository = studentProfileRepository,
                    authRepository = authRepository,
                    matchScoreProvider = matchScoreProvider
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
