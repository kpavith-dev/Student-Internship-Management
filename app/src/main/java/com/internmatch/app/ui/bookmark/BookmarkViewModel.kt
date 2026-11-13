package com.internmatch.app.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.internmatch.app.domain.model.SkillProficiency
import com.internmatch.app.domain.repository.AuthRepository
import com.internmatch.app.domain.repository.BookmarkRepository
import com.internmatch.app.domain.repository.InternshipRepository
import com.internmatch.app.domain.repository.StudentProfileRepository
import com.internmatch.app.domain.service.MatchScoreProvider
import com.internmatch.app.ui.search.InternshipItemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookmarkViewModel(
    private val bookmarkRepository: BookmarkRepository,
    private val internshipRepository: InternshipRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val authRepository: AuthRepository,
    private val matchScoreProvider: MatchScoreProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarkUiState())
    val uiState: StateFlow<BookmarkUiState> = _uiState.asStateFlow()

    private var studentSkills: List<SkillProficiency> = emptyList()

    init {
        loadBookmarks()
    }

    fun loadBookmarks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.update { it.copy(isLoading = false, bookmarkedInternships = emptyList()) }
                    return@launch
                }

                val profileResult = studentProfileRepository.getStudentProfile(currentUser.id)
                studentSkills = profileResult.getOrNull()?.skillProficiencies ?: emptyList()

                val bookmarksResult = bookmarkRepository.getBookmarks(currentUser.id)
                if (bookmarksResult.isFailure) {
                    val err = bookmarksResult.exceptionOrNull()?.message ?: "Failed to load bookmarks."
                    _uiState.update { it.copy(isLoading = false, errorMessage = err) }
                    return@launch
                }

                val bookmarks = bookmarksResult.getOrDefault(emptyList())
                val bookmarkedIds = bookmarks.map { it.internshipId }.toSet()

                val internshipsResult = internshipRepository.getApprovedInternships()
                val allInternships = internshipsResult.getOrDefault(emptyList())

                val bookmarkedItems = allInternships.filter { bookmarkedIds.contains(it.id) }.map { internship ->
                    val score = matchScoreProvider.calculateMatchScore(studentSkills, internship.requiredSkills)
                    InternshipItemUiState(
                        internship = internship,
                        isBookmarked = true,
                        matchScore = score
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bookmarkedInternships = bookmarkedItems,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "An error occurred.") }
            }
        }
    }

    fun removeBookmark(internshipId: String) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser() ?: return@launch
            val result = bookmarkRepository.removeBookmark(currentUser.id, internshipId)
            if (result.isSuccess) {
                _uiState.update { current ->
                    current.copy(
                        bookmarkedInternships = current.bookmarkedInternships.filter { it.internship.id != internshipId }
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to remove bookmark.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
