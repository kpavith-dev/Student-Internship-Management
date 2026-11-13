package com.internmatch.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.internmatch.app.domain.model.Internship
import com.internmatch.app.domain.model.SkillProficiency
import com.internmatch.app.domain.repository.AuthRepository
import com.internmatch.app.domain.repository.BookmarkRepository
import com.internmatch.app.domain.repository.InternshipRepository
import com.internmatch.app.domain.repository.StudentProfileRepository
import com.internmatch.app.domain.service.MatchScoreProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InternshipSearchViewModel(
    private val internshipRepository: InternshipRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val authRepository: AuthRepository,
    private val matchScoreProvider: MatchScoreProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(InternshipSearchUiState())
    val uiState: StateFlow<InternshipSearchUiState> = _uiState.asStateFlow()

    private var allApprovedInternships: List<Internship> = emptyList()
    private var studentSkills: List<SkillProficiency> = emptyList()
    private var bookmarkedIds: Set<String> = emptySet()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val profileResult = studentProfileRepository.getStudentProfile(currentUser.id)
                    studentSkills = profileResult.getOrNull()?.skillProficiencies ?: emptyList()

                    val bookmarksResult = bookmarkRepository.getBookmarks(currentUser.id)
                    bookmarkedIds = bookmarksResult.getOrDefault(emptyList()).map { it.internshipId }.toSet()
                }

                val internshipsResult = internshipRepository.getApprovedInternships()
                if (internshipsResult.isSuccess) {
                    allApprovedInternships = internshipsResult.getOrDefault(emptyList())

                    val domains = allApprovedInternships.map { it.domain }.filter { it.isNotBlank() }.distinct().sorted()
                    val districts = allApprovedInternships.map { it.district }.filter { it.isNotBlank() }.distinct().sorted()

                    _uiState.update { current ->
                        current.copy(
                            availableDomains = domains,
                            availableDistricts = districts,
                            isLoading = false
                        )
                    }
                    applyFilters()
                } else {
                    val error = internshipsResult.exceptionOrNull()?.message ?: "Failed to load internships."
                    _uiState.update { it.copy(isLoading = false, errorMessage = error) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "An unexpected error occurred.") }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(filterState = it.filterState.copy(query = query)) }
        applyFilters()
    }

    fun updateDomain(domain: String) {
        _uiState.update { it.copy(filterState = it.filterState.copy(domain = domain)) }
        applyFilters()
    }

    fun updateDistrict(district: String) {
        _uiState.update { it.copy(filterState = it.filterState.copy(district = district)) }
        applyFilters()
    }

    fun updateWorkMode(workMode: String) {
        _uiState.update { it.copy(filterState = it.filterState.copy(workMode = workMode)) }
        applyFilters()
    }

    fun updateMinMatchScore(minScore: Double) {
        _uiState.update { it.copy(filterState = it.filterState.copy(minMatchScore = minScore)) }
        applyFilters()
    }

    fun clearFilters() {
        _uiState.update { it.copy(filterState = SearchFilterUiState()) }
        applyFilters()
    }

    fun toggleBookmark(internshipId: String) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser() ?: return@launch
            val isCurrentlyBookmarked = bookmarkedIds.contains(internshipId)

            val result = if (isCurrentlyBookmarked) {
                bookmarkRepository.removeBookmark(currentUser.id, internshipId)
            } else {
                bookmarkRepository.addBookmark(currentUser.id, internshipId)
            }

            if (result.isSuccess) {
                bookmarkedIds = if (isCurrentlyBookmarked) {
                    bookmarkedIds - internshipId
                } else {
                    bookmarkedIds + internshipId
                }
                applyFilters()
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to update bookmark.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun applyFilters() {
        val filter = _uiState.value.filterState

        val filteredItems = allApprovedInternships.mapNotNull { internship ->
            val matchesQuery = filter.query.isBlank() ||
                internship.title.contains(filter.query, ignoreCase = true) ||
                internship.companyName.contains(filter.query, ignoreCase = true) ||
                internship.description.contains(filter.query, ignoreCase = true)

            val matchesDomain = filter.domain.isBlank() || internship.domain.equals(filter.domain, ignoreCase = true)
            val matchesDistrict = filter.district.isBlank() || internship.district.equals(filter.district, ignoreCase = true)
            val matchesWorkMode = filter.workMode.isBlank() || internship.workMode.equals(filter.workMode, ignoreCase = true)

            if (!matchesQuery || !matchesDomain || !matchesDistrict || !matchesWorkMode) {
                return@mapNotNull null
            }

            val score = matchScoreProvider.calculateMatchScore(studentSkills, internship.requiredSkills)
            if (filter.minMatchScore > 0.0 && (score == null || score < filter.minMatchScore)) {
                return@mapNotNull null
            }

            InternshipItemUiState(
                internship = internship,
                isBookmarked = bookmarkedIds.contains(internship.id),
                matchScore = score
            )
        }

        _uiState.update { it.copy(internships = filteredItems) }
    }
}
